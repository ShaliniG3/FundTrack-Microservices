package com.cts.fundtrack.disbursement.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Background scheduled task that manages the automatic lifecycle transitions of
 * grant disbursement installments.
 * <p>
 * This component runs a nightly cron job to identify all installments that are in
 * {@code SCHEDULED} status and whose scheduled date has arrived or passed. It transitions
 * those installments to {@code PENDING} status, signalling to Finance Officers that
 * payment processing can now be initiated for those installments.
 * </p>
 * <p>
 * This automated promotion prevents installments from silently remaining in
 * {@code SCHEDULED} status past their due dates and ensures the financial dashboard
 * always reflects an accurate picture of actionable obligations.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisbursementScheduler {

    private final DisbursementRepository disbursementRepo;

    /**
     * Nightly job that promotes due disbursement installments from {@code SCHEDULED}
     * to {@code PENDING} status.
     * <p>
     * Executes at midnight (00:00) every day via the cron expression {@code 0 0 0 * * *}.
     * Fetches all {@link com.cts.fundtrack.disbursement.models.Disbursement} records with
     * status {@code SCHEDULED} and a {@code scheduledDate} on or before today, then
     * bulk-updates their status to {@code PENDING} and persists the changes in a single
     * transactional batch.
     * </p>
     */
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void processScheduledDisbursements() {
        log.info("Starting daily disbursement status update check...");

        LocalDate today = LocalDate.now();
        
        // Fetch all installments that are due but still marked as SCHEDULED
        List<Disbursement> dueInstallments = disbursementRepo
            .findByStatusAndScheduledDateLessThanEqual(DisbursementStatus.SCHEDULED, today);

        if (!dueInstallments.isEmpty()) {
            dueInstallments.forEach(dis -> {
                dis.setStatus(DisbursementStatus.PENDING);
                log.debug("Updated Disbursement ID {} to PENDING", dis.getDisbursementId());
            });
            
            disbursementRepo.saveAll(dueInstallments);
            log.info("Successfully transitioned {} installments to PENDING.", dueInstallments.size());
        } else {
            log.info("No disbursements due for update today.");
        }
    }
}

