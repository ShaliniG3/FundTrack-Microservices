package com.cts.fundtrack.disbursement.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.ProgramMetadataDTO;
import com.cts.fundtrack.common.exceptions.InvalidProgramStateException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.common.models.enums.PaymentFrequency;
import com.cts.fundtrack.disbursement.client.ApplicationClient;
import com.cts.fundtrack.disbursement.client.ProgramClient;
import com.cts.fundtrack.disbursement.mapper.ModuleMapper;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing the lifecycle of grant disbursements and budget allocations.
 * <p>
 * This service coordinates with the Program and Application microservices to finalize 
 * financial distributions for closed programs, generate structured installment schedules, 
 * and ensure all payments are audited for compliance.
 * </p>
 *
 * @author FundTrack Development Team
 * @version 1.4
 * @since 2026-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisbursementServiceImpl implements DisbursementService {

    private final DisbursementRepository disbursementRepository;
    private final ModuleMapper mapper;
    private final ProgramClient programClient;
    private final ApplicationClient applicationClient;
    private final NotificationClient notificationClient;
    private final HttpServletRequest request;

    /**
     * Extracts the Unique Identifier of the currently authenticated Admin/Staff from request headers.
     * @return UUID of the logged-in user.
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Retrieves the chronological disbursement schedule for a specific funding application.
     *
     * @param applicationId The unique identifier of the target application.
     * @return A list of {@link Disbursement} installments ordered by scheduled date.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.DISBURSEMENT)
    public List<Disbursement> getScheduleById(UUID applicationId) {
        log.debug("Fetching installment schedule for Application ID: {}", applicationId);
        return disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId);
    }

    /**
     * Calculates the total remaining financial liability for a specific application.
     *
     * @param applicationId The unique identifier of the target application.
     * @return The sum of all unpaid and non-cancelled installment amounts.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.DISBURSEMENT)
    public Double getRemainingBalance(UUID applicationId) {
        log.debug("Calculating outstanding balance for Application ID: {}", applicationId);
        return disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId).stream()
                .filter(d -> !DisbursementStatus.PAID.equals(d.getStatus()) && !DisbursementStatus.CANCELLED.equals(d.getStatus()))
                .mapToDouble(Disbursement::getAmount)
                .sum();
    }

    /**
     * Finalizes the budget for a closed program and generates installment schedules for approved winners.
     * <p>
     * Performs cross-service validation to ensure the program is CLOSED and all reviews are completed.
     * Generates a notification for the logged-in user upon successful schedule creation.
     * </p>
     *
     * @param programId The program whose budget is to be finalized.
     * @param frequency The interval between installments (e.g., MONTHLY, QUARTERLY).
     * @param numberOfPayments The total number of payments per winner.
     * @return A summary of the newly created disbursement schedules.
     * @throws InvalidProgramStateException if the program is not in a terminal 'CLOSED' state or has pending reviews.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.DISBURSEMENT)
    public List<DisbursementResponseDTO> finalizeAndSplitBudget(UUID programId, PaymentFrequency frequency, int numberOfPayments) {
        log.info("Financial Finalization Triggered | Program: {} | Interval: {}", programId, frequency);

        // 1. Remote Validation: Program Status
        ProgramMetadataDTO program = programClient.getProgramById(programId);
        if (!"CLOSED".equalsIgnoreCase(program.getStatus())) {
            log.error("Budget Split Blocked: Program {} is currently in status {}", programId, program.getStatus());
            throw new InvalidProgramStateException("Budget Split Rejected: Program must be in CLOSED status.");
        }

        // 2. Remote Validation: Pending Reviews
        if (applicationClient.hasPendingReviews(programId)) {
            log.warn("Budget Split Blocked: Unfinished evaluations found for Program {}", programId);
            throw new InvalidProgramStateException("Budget Split Rejected: All applications must be reviewed before finalization.");
        }

        // 3. Identification of Approved Recipients
        List<UUID> winnerIds = applicationClient.getApprovedApplicationIds(programId);
        if (winnerIds.isEmpty()) {
            log.info("Process Terminated: No approved winners found for Program ID: {}", programId);
            return new ArrayList<>();
        }

        double individualShare = program.getBudget() / winnerIds.size();
        int interval = calculateMonthInterval(frequency);
        List<Disbursement> allNewEntities = new ArrayList<>();

        for (UUID appId : winnerIds) {
            boolean alreadyHasSchedule = !disbursementRepository.findByApplicationId(appId).isEmpty();

            if (!alreadyHasSchedule) {
                // Generate and Persist Schedule
                List<Disbursement> savedBatch = createInstallments(appId, programId, individualShare, numberOfPayments, interval);
                allNewEntities.addAll(savedBatch);

                // Update External Application State
                applicationClient.updateApplicationStatus(appId, "ACCEPTED");

                // 🚀 Transactional Confirmation: Target the currently logged-in Admin/Staff
                sendInternalNotification(appId, 
                    String.format("System Confirmation: Budget split complete for Program '%s'. Installment plan created for Application %s.", 
                    program.getName(), appId), 
                    NotificationCategory.ACCEPTED);
            }
        }

        return allNewEntities.stream()
                .map(mapper::toDisbursementResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Internal logic for determining the monthly increment based on payment frequency.
     */
    private int calculateMonthInterval(PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case HALF_YEARLY -> 6;
            case YEARLY -> 12;
        };
    }

    /**
     * Generates a batch of disbursement installments with calculated dates and amounts.
     */
    private List<Disbursement> createInstallments(UUID appId, UUID programId, double totalAmount, int count, int monthInterval) {
        double amountPerInstallment = Math.round((totalAmount / count) * 100.0) / 100.0;
        LocalDate startDate = LocalDate.now();
        List<Disbursement> batch = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Disbursement d = Disbursement.builder()
                    .applicationId(appId)
                    .programId(programId)
                    .amount(amountPerInstallment)
                    .scheduledDate(startDate.plusMonths((long) i * monthInterval))
                    .status(DisbursementStatus.SCHEDULED)
                    .build();
            batch.add(d);
        }

        log.info("Schedule Generated: {} installments created for AppID: {}", count, appId);
        return disbursementRepository.saveAll(batch);
    }

    /**
     * Dispatcher for internal microservice notifications. 
     * Targets the authenticated user context to provide a confirmation receipt of the action.
     */
    private void sendInternalNotification(UUID appId, String message, NotificationCategory category) {
        UUID currentLoggedInUser = getCurrentUserId();
        
        if (currentLoggedInUser == null) {
            log.warn("Notification skipped: No authenticated user ID found in request headers.");
            return;
        }

        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(currentLoggedInUser) // 🚀 Confirmation sent to the Admin
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            
            notificationClient.sendNotification(notification);
            log.debug("Transactional confirmation dispatched to user: {}", currentLoggedInUser);
        } catch (Exception e) {
            log.error("Internal Notification Error: Unable to reach Notification Service for user {}. Reason: {}", 
                      currentLoggedInUser, e.getMessage());
        }
    }
}