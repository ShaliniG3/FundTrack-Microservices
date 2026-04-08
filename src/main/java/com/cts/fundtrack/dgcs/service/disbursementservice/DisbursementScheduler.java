package com.cts.fundtrack.dgcs.service.disbursementservice;

import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Background scheduler to manage the lifecycle of grant disbursements.
 * Ensures accountability by automatically updating statuses as dates arrive[cite: 6, 87].
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisbursementScheduler {

    private final DisbursementRepository disbursementRepo;

    /**
     * Cron expression: Runs at 00:00 (midnight) every day.
     * Logic: Finds all 'SCHEDULED' disbursements where the date is today or earlier 
     * and flips them to 'PENDING'
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


