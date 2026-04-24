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
import com.cts.fundtrack.common.exceptions.ServiceUnavailableException;
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
 * Primary implementation of the {@link DisbursementService} interface.
 * <p>
 * This service contains the core financial logic of the FundTrack disbursement workflow.
 * It orchestrates the budget-splitting algorithm, computes outstanding balances, and
 * coordinates with the Program Service and Application Service via Feign clients to
 * ensure all pre-conditions are met before installment schedules are generated.
 * </p>
 * <p>
 * All mutating operations are transactional and emit audit events via the
 * {@link com.cts.fundtrack.common.aspect.Auditable} AOP annotation.
 * </p>
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
     * Extracts the authenticated user's UUID from the {@code X-User-Id} header
     * injected by the API Gateway.
     *
     * @return the current user's {@link UUID}, or {@code null} if the header is absent
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.DISBURSEMENT)
    public List<Disbursement> getScheduleById(UUID applicationId) {
        log.debug("Fetching installment schedule for Application ID: {}", applicationId);
        return disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId);
    }

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
     * Orchestrates the full budget-splitting and disbursement-scheduling workflow for a
     * closed grant program.
     *
     * <p>Pre-conditions enforced before any financial records are created:
     * <ol>
     *   <li>The Program Service must be reachable — if the circuit breaker is open,
     *       a {@link ServiceUnavailableException} is thrown and the operation is aborted
     *       with a {@code 503} response. The program's persisted status is never read
     *       from a fallback stub, preventing the previous
     *       {@code "Budget Split Blocked: SERVICE_UNAVAILABLE"} error.</li>
     *   <li>The program must be in {@code CLOSED} status — enforced against the real
     *       status returned by the Program Service.</li>
     *   <li>All submitted applications must have been reviewed — no pending evaluations
     *       may remain.</li>
     *   <li>At least one application must have been approved.</li>
     * </ol>
     * </p>
     *
     * <p>For each approved application that does not already have a disbursement schedule,
     * the total budget is divided equally among all winners and an installment plan is
     * generated at the requested {@code frequency} and {@code numberOfPayments}.</p>
     *
     * @param programId        the UUID of the grant program to finalize
     * @param frequency        the payout cadence ({@code MONTHLY}, {@code QUARTERLY},
     *                         {@code HALF_YEARLY}, or {@code YEARLY})
     * @param numberOfPayments the total number of installments per approved applicant
     * @return the list of newly created {@link DisbursementResponseDTO} records;
     *         empty if no approved applications exist
     * @throws ServiceUnavailableException   if the Program Service is currently unreachable
     * @throws InvalidProgramStateException  if the program is not {@code CLOSED} or if
     *                                       pending application reviews remain
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.DISBURSEMENT)
    public List<DisbursementResponseDTO> finalizeAndSplitBudget(UUID programId, PaymentFrequency frequency, int numberOfPayments) {
        log.info("Financial Finalization Triggered | Program: {} | Interval: {}", programId, frequency);

        ProgramMetadataDTO program;
        try {
            program = programClient.getProgramById(programId);
        } catch (ServiceUnavailableException e) {
            // Program Service is down — abort cleanly without touching any state.
            // The program's real status in the DB is unaffected; the operation can
            // be retried once the service recovers.
            log.warn("Budget Split Deferred: Program Service unavailable for programId={}", programId);
            throw e; // propagates to global exception handler → 503 to client
        }

        if (!"CLOSED".equalsIgnoreCase(program.getStatus())) {
            log.error("Budget Split Blocked: Program {} is currently in status {}", programId, program.getStatus());
            throw new InvalidProgramStateException("Budget Split Rejected: Program must be in CLOSED status.");
        }

        if (applicationClient.hasPendingReviews(programId)) {
            log.warn("Budget Split Blocked: Unfinished evaluations found for Program {}", programId);
            throw new InvalidProgramStateException("Budget Split Rejected: All applications must be reviewed before finalization.");
        }

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
                List<Disbursement> savedBatch = createInstallments(appId, programId, individualShare, numberOfPayments, interval);
                allNewEntities.addAll(savedBatch);

                applicationClient.updateApplicationStatus(appId, "ACCEPTED");

                // Notify the applicant that their funds have been scheduled
                UUID applicantUserId = applicationClient.getApplicationMetadata(appId).getApplicantUserId();
                sendInternalNotification(applicantUserId, appId,
                        String.format("Congratulations! Your grant application for Program '%s' has been accepted. Your installment plan is now active.",
                                program.getName()),
                        NotificationCategory.ACCEPTED);

                // Confirm the operation to the logged-in finance officer
                sendInternalNotification(getCurrentUserId(), appId,
                        String.format("System Confirmation: Budget split complete for Program '%s'. Installment plan created for Application %s.",
                                program.getName(), appId),
                        NotificationCategory.DISBURSEMENT);
            }
        }

        return allNewEntities.stream()
                .map(mapper::toDisbursementResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converts a {@link PaymentFrequency} enum value into the equivalent number of months
     * between consecutive installments.
     *
     * @param frequency the desired payout cadence
     * @return the number of calendar months between each installment
     *         (1 for MONTHLY, 3 for QUARTERLY, 6 for HALF_YEARLY, 12 for YEARLY)
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
     * Generates and persists a batch of evenly-spaced disbursement installments for a
     * single approved application.
     * <p>
     * The total amount is divided equally across the requested number of installments,
     * with each installment's scheduled date offset by {@code monthInterval} months
     * from the previous one, starting from today. All installments are created with
     * {@code SCHEDULED} status.
     * </p>
     *
     * @param appId           the UUID of the approved grant application
     * @param programId       the UUID of the grant program being finalized
     * @param totalAmount     the full grant award amount allocated to this applicant
     * @param count           the total number of installments to create
     * @param monthInterval   the number of months between consecutive installments
     * @return the list of persisted {@link Disbursement} entities
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
     * Dispatches a transactional confirmation notification to the currently authenticated
     * Finance Officer after a budget-split operation completes.
     * <p>
     * Notification failures are caught and logged as non-critical errors so that they
     * never roll back the enclosing financial transaction.
     * </p>
     *
     * @param appId    the UUID of the application for which the notification is contextualised
     * @param message  the human-readable notification message body
     * @param category the {@link NotificationCategory} used to classify the alert in the UI
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        if (userId == null) {
            log.warn("Notification skipped: No authenticated user ID found in request headers.");
            return;
        }

        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(userId)
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            notificationClient.sendNotification(notification);
            log.debug("Notification dispatched to user: {}", userId);
        } catch (Exception e) {
            log.error("Internal Notification Error: Unable to reach Notification Service for user {}. Reason: {}",
                    userId, e.getMessage());
        }
    }
}