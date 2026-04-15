package com.cts.fundtrack.disbursement.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable; // 👈 IMPORT YOUR COMMON ANNOTATION
import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.dto.ProgramMetadataDTO;
import com.cts.fundtrack.common.exceptions.InvalidProgramStateException;
import com.cts.fundtrack.common.models.enums.ActionType; // 👈 IMPORT ENUMS
import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.PaymentFrequency;
import com.cts.fundtrack.disbursement.client.ApplicationClient;
import com.cts.fundtrack.disbursement.client.ProgramClient;
import com.cts.fundtrack.disbursement.mapper.ModuleMapper;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing grant disbursements.
 * Fully audited for financial compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementServiceImpl implements DisbursementService {

    private final DisbursementRepository disbursementRepository;
    private final ModuleMapper mapper;
    private final ProgramClient programClient;
    private final ApplicationClient applicationClient;

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.DISBURSEMENT) // 👈 AUDIT ENABLED
    public List<Disbursement> getScheduleById(UUID applicationId) {
        return disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.DISBURSEMENT) // 👈 AUDIT ENABLED
    public Double getRemainingBalance(UUID applicationId) {
        return disbursementRepository.findByApplicationIdOrderByScheduledDateAsc(applicationId).stream()
                .filter(d -> !DisbursementStatus.PAID.equals(d.getStatus()) && !DisbursementStatus.CANCELLED.equals(d.getStatus()))
                .mapToDouble(Disbursement::getAmount)
                .sum();
    }

    /**
     * This is the "Big One." It splits the budget and creates the schedule.
     * Audited as a CREATE action for the Disbursement entity.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.DISBURSEMENT) // 👈 AUDIT ENABLED
    public List<DisbursementResponseDTO> finalizeAndSplitBudget(UUID programId, PaymentFrequency frequency, int numberOfPayments) {
        log.info("Microservice Sequence: Finalizing Budget | Program ID: {}", programId);

        // 1. Verify Program via Feign Client
        ProgramMetadataDTO program = programClient.getProgramById(programId);

        if (!"CLOSED".equalsIgnoreCase(program.getStatus())) {
            throw new InvalidProgramStateException("Budget Split Rejected: Program must be CLOSED.");
        }

        // 2. Security Check: Are there pending reviews?
        if (applicationClient.hasPendingReviews(programId)) {
            throw new InvalidProgramStateException("Budget Split Rejected: Pending reviews exist.");
        }

        // 3. Fetch Approved Application IDs via Feign
        List<UUID> winnerIds = applicationClient.getApprovedApplicationIds(programId);

        if (winnerIds.isEmpty()) {
            log.info("Process halted: No approved winners for Program: {}.", programId);
            return new ArrayList<>();
        }

        double individualShare = program.getBudget() / winnerIds.size();
        int interval = calculateMonthInterval(frequency);
        List<Disbursement> allNewEntities = new ArrayList<>();

        for (UUID appId : winnerIds) {
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

    private int calculateMonthInterval(PaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> 1;
            case QUARTERLY -> 3;
            case HALF_YEARLY -> 6;
            case YEARLY -> 12;
        };
    }

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

        log.info("Saving {} installments for Application ID: {}", count, appId);
        return disbursementRepository.saveAll(batch);
    }
}