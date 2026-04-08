package com.cts.fundtrack.dgcs.service.disbursementservice;

import com.cts.fundtrack.dgcs.client.applicationclient.ApplicationClient;
import com.cts.fundtrack.dgcs.client.dto.ProgramMetadataDTO;
import com.cts.fundtrack.dgcs.client.programclient.ProgramClient;
import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementResponseDTO;
import com.cts.fundtrack.dgcs.exception.InvalidProgramStateException;

import com.cts.fundtrack.dgcs.mapper.ModuleMapper;

import com.cts.fundtrack.dgcs.model.Disbursement;

import com.cts.fundtrack.dgcs.model.enums.*;

import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;
//import com.cts.fundtrack.dgcs.repository.programrepository.ProgramRepository;
//import com.cts.fundtrack.dgcs.util.AuditHelper;
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
 * Service implementation for managing grant disbursements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementServiceImpl implements DisbursementService {
    private final DisbursementRepository disbursementRepository;
    //private final ApplicationRepository applicationRepository;
  //  private final ProgramRepository programRepository;
//    private final AuditHelper auditHelper;
//    private final NotificationHelper notificationHelper;
    private final ModuleMapper mapper;
    private final ProgramClient programClient;
    private final ApplicationClient applicationClient;


    @Override
    @Transactional(readOnly = true)
    public List<Disbursement> getScheduleById(UUID applicationId) {
        return disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getRemainingBalance(UUID applicationId) {
        return disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId).stream()
                .filter(d -> !DisbursementStatus.PAID.equals(d.getStatus()) && !DisbursementStatus.CANCELLED.equals(d.getStatus()))
                .mapToDouble(Disbursement::getAmount)
                .sum();
    }

    @Transactional
    @Override
    public List<DisbursementResponseDTO> finalizeAndSplitBudget(UUID programId, PaymentFrequency frequency, int numberOfPayments) {
        log.info("Microservice Sequence: Finalizing Budget | Program ID: {}", programId);

        // 1. Verify Program via Feign Client (replacing programRepository)
        ProgramMetadataDTO program = programClient.getProgramById(programId);

        if (!"CLOSED".equalsIgnoreCase(program.getStatus())) {
            throw new InvalidProgramStateException("Budget Split Rejected: Program must be CLOSED.");
        }

        // 2. Security Check: Are there pending reviews?
        // This now requires a Feign call to the Application Service
        if (applicationClient.hasPendingReviews(programId)) {
            throw new InvalidProgramStateException("Budget Split Rejected: Pending reviews exist.");
        }

        // 3. Fetch Approved Application IDs (not entities) via Feign
        List<UUID> winnerIds = applicationClient.getApprovedApplicationIds(programId);

        if (winnerIds.isEmpty()) {
            log.info("Process halted: No approved winners for Program: {}.", programId);
            return new ArrayList<>();
        }

        double individualShare = program.getBudget() / winnerIds.size();
        int interval = calculateMonthInterval(frequency);
        List<Disbursement> allNewEntities = new ArrayList<>();

        for (UUID appId : winnerIds) {
            // Check local DB if schedule already exists for this UUID
            boolean alreadyHasSchedule = !disbursementRepository.findByApplicationId(appId).isEmpty();

            if (!alreadyHasSchedule) {
                // Create the schedule locally
                List<Disbursement> savedBatch = createInstallments(appId, programId, individualShare, numberOfPayments, interval);
                allNewEntities.addAll(savedBatch);

                // 4. Update Remote Application Status via Feign
                applicationClient.updateApplicationStatus(appId, "ACCEPTED");
            }
        }

        return allNewEntities.stream()
                .map(mapper::toDisbursementResponseDTO)
                .collect(Collectors.toList());
    }

    private boolean hasPendingReviews(UUID programId) {
        // We ask the Application Service to check its own database
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
                    .applicationId(appId)      // Use the UUID, not the Object
                    .programId(programId)          // Also store the Program ID for reporting
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