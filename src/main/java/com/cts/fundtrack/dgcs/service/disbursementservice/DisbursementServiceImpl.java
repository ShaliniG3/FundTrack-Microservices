package com.cts.fundtrack.dgcs.service.disbursementservice;

import com.cts.fundtrack.dgcs.client.applicationclient.ApplicationClient;
import com.cts.fundtrack.dgcs.client.dto.ProgramMetadataDTO;
import com.cts.fundtrack.dgcs.client.programclient.ProgramClient;

import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementResponseDTO;
import com.cts.fundtrack.dgcs.exception.DisbursementDataAccessException;
import com.cts.fundtrack.dgcs.exception.DisbursementPersistenceException;
import com.cts.fundtrack.dgcs.exception.InvalidProgramStateException;

import com.cts.fundtrack.dgcs.mapper.ModuleMapper;

import com.cts.fundtrack.dgcs.model.Disbursement;

import com.cts.fundtrack.dgcs.model.enums.*;

import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;

//import com.cts.fundtrack.dgcs.util.NotificationHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing the financial lifecycle of grant disbursements.
 * <p>
 * This service acts as the financial orchestrator, responsible for:
 * <ul>
 * <li>Tracking payment installments against grant applications.</li>
 * <li>Verifying funding availability through the Program Service.</li>
 * <li>Ensuring metadata consistency between financial records and external application data.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementServiceImpl implements DisbursementService {
    private final DisbursementRepository disbursementRepository;
    private final ModuleMapper mapper;
    private final ProgramClient programClient;
    private final ApplicationClient applicationClient;

        /**
         * Retrieves the chronological payment schedule for a specific grant application.
         * <p>
         * Sequence: Entity Resolution -> Schedule Retrieval -> Sort Verification.
         * </p>
         *
         * @param applicationId The unique identifier for the grant application.
         * @return A {@link List} of {@link Disbursement} entities ordered by their scheduled date.
         * @throws DisbursementDataAccessException If the database query for the schedule fails.
         */
        @Override
        @Transactional(readOnly = true)
        public List<Disbursement> getScheduleById(UUID applicationId) {

            log.info("Process Start: Fetching Disbursement Schedule | Application: {}", applicationId);

            try {
                log.debug("Validation: Verifying schedule records for AppID: {}", applicationId);

                List<Disbursement> schedule = disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId);

                if (schedule == null || schedule.isEmpty()) {
                    log.warn("Lookup Notice: No disbursements found for Application: {}", applicationId);
                    // Depending on your business logic, you may throw an ApplicationNotFoundException here
                }

                log.info("Process Complete: Schedule retrieved successfully. Count: {} for Application {}",
                        (schedule != null ? schedule.size() : 0), applicationId);

                return schedule;

            } catch (Exception e) {
                log.error("Database Failure: Could not retrieve schedule metadata for ID: {}. Error: {}",
                        applicationId, e.getMessage());

                throw new DisbursementDataAccessException("System failed to synchronize schedule state: " + e.getMessage());
            }
        }


    /**
     * Calculates the outstanding financial balance for a specific grant application.
     * <p>
     * Sequence: Schedule Retrieval -> Status Filtering (Excluding PAID/CANCELLED) -> Aggregation.
     * </p>
     *
     * @param applicationId The unique identifier for the grant application.
     * @return The sum of all pending or scheduled disbursement amounts.
     * @throws DisbursementDataAccessException If the database fails to retrieve the disbursement list.
     */
    @Override
    @Transactional(readOnly = true)
    public Double getRemainingBalance(UUID applicationId) {

        log.info("Process Start: Calculating Remaining Balance | Application: {}", applicationId);

        try {
            log.debug("Logic Execution: Filtering active installments for AppID: {}", applicationId);

            double balance = disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId).stream()
                    .filter(d -> !DisbursementStatus.PAID.equals(d.getStatus())
                            && !DisbursementStatus.CANCELLED.equals(d.getStatus()))
                    .mapToDouble(Disbursement::getAmount)
                    .sum();

            log.info("Process Complete: Balance calculation finished for Application {}. Result: {}",
                    applicationId, balance);

            return balance;

        } catch (Exception e) {
            log.error("Calculation Failure: Could not aggregate balance for Application: {}. Reason: {}",
                    applicationId, e.getMessage());

            throw new DisbursementDataAccessException("System failed to calculate outstanding balance: " + e.getMessage());
        }
    }

    /**
     * Orchestrates the final budget allocation and installment generation for a closed program.
     * <p>
     * Sequence: Program Metadata Resolution -> Review Validation -> Winner Resolution ->
     * Pro-rata Calculation -> Installment Generation -> Remote Status Synchronization.
     * </p>
     *
     * @param programId        The unique identifier of the funding program.
     * @param frequency        The payment recurrence interval (e.g., MONTHLY, QUARTERLY).
     * @param numberOfPayments The total count of installments to be generated per winner.
     * @return A {@link List} of {@link DisbursementResponseDTO} representing the created schedules.
     * @throws InvalidProgramStateException If the program is not in a CLOSED state or has unresolved reviews.
     * @throws DisbursementPersistenceException If the local database synchronization fails.
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public List<DisbursementResponseDTO> finalizeAndSplitBudget(UUID programId, PaymentFrequency frequency, int numberOfPayments) {

        log.info("Process Start: Budget Finalization & Splitting | Program: {}", programId);

        try {
            // 1. VERIFY PROGRAM STATUS VIA FEIGN
            log.debug("Resolution: Fetching program metadata for ID: {}", programId);
            ProgramMetadataDTO program = programClient.getProgramById(programId);

            if (program == null || !"CLOSED".equalsIgnoreCase(program.getStatus())) {
                log.warn("State Conflict: Program {} must be CLOSED to split budget. Current Status: {}",
                        programId, (program != null ? program.getStatus() : "NULL"));
                throw new InvalidProgramStateException("Budget Split Rejected: Program must be CLOSED.");
            }

            // 2. SECURITY CHECK: PENDING REVIEWS
            log.debug("Validation: Checking for pending reviews in Application Service for Program: {}", programId);
            if (applicationClient.hasPendingReviews(programId)) {
                log.warn("Validation Rejected: Program {} has unresolved application reviews.", programId);
                throw new InvalidProgramStateException("Budget Split Rejected: Pending reviews exist.");
            }

            // 3. FETCH APPROVED BENEFICIARIES
            log.debug("Resolution: Identifying approved application IDs via Feign.");
            List<UUID> winnerIds = applicationClient.getApprovedApplicationIds(programId);

            if (winnerIds == null || winnerIds.isEmpty()) {
                log.info("Process Halted: No approved winners found for Program: {}.", programId);
                return new ArrayList<>();
            }

            // 4. FINANCIAL CALCULATION
            double individualShare = program.getBudget() / winnerIds.size();
            int interval = calculateMonthInterval(frequency);
            List<Disbursement> allNewEntities = new ArrayList<>();

            log.info("Logic Execution: Distributing budget of {} across {} winners.",
                    program.getBudget(), winnerIds.size());

            // 5. SCHEDULE GENERATION & REMOTE SYNC
            for (UUID appId : winnerIds) {
                // Check local DB for existing schedule
                boolean alreadyHasSchedule = !disbursementRepository.findByApplicationId(appId).isEmpty();

                if (!alreadyHasSchedule) {
                    log.debug("Persistence: Generating installments for Application: {}", appId);
                    List<Disbursement> savedBatch = createInstallments(appId, programId, individualShare, numberOfPayments, interval);
                    allNewEntities.addAll(savedBatch);

                    // Update Remote Application Status
                    log.debug("Sync: Updating remote status to ACCEPTED for Application: {}", appId);
                    applicationClient.updateApplicationStatus(appId, "ACCEPTED");
                } else {
                    log.warn("Duplicate Prevention: Schedule already exists for AppID: {}. Skipping generation.", appId);
                }
            }

            log.info("Process Complete: Budget finalized for Program {}. Generated {} records.",
                    programId, allNewEntities.size());

            return allNewEntities.stream()
                    .map(mapper::toDisbursementResponseDTO)
                    .collect(Collectors.toList());

        } catch (InvalidProgramStateException e) {
            // Rethrow business rule violations directly
            throw e;
        } catch (Exception e) {
            log.error("Orchestration Failure: Critical error during budget finalization for Program: {}. Reason: {}",
                    programId, e.getMessage());

            throw new DisbursementPersistenceException("System failed to finalize and split budget: " + e.getMessage());
        }
    }

    private boolean hasPendingReviews(UUID programId) {
        return applicationClient.hasPendingReviews(programId);
    }
    private int calculateMonthInterval(PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case HALF_YEARLY -> 6;
            case YEARLY -> 12;
        };
    }

    private List<Disbursement> createInstallments(UUID appId, UUID programId, double totalAmount, int count, int monthInterval) {
        // 1. Calculate the split (rounding to 2 decimal places for currency)
        double amountPerInstallment = Math.round((totalAmount / count) * 100.0) / 100.0;

        LocalDate startDate = LocalDate.now();
        List<Disbursement> batch = new ArrayList<>();

        // 2. Loop to create the schedule
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

        // 3. Save to the local Disbursement database
        log.info("Saving {} installments for Application ID: {}", count, appId);
        return disbursementRepository.saveAll(batch);
    }

}