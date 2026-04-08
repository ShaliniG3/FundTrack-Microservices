package com.cts.fundtrack.dgcs.service.validation;

import com.cts.fundtrack.dgcs.exception.ProgramLifecycleException;
import com.cts.fundtrack.dgcs.exception.ReportingWindowException;
import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingWindowValidator {

    private final DisbursementRepository disbursementRepository;

    public void validateWindow(UUID applicationId) {
        log.debug("Initiating Dynamic Window Validation for Application: {}", applicationId);

        List<Disbursement> schedule = disbursementRepository
                .findByApplicationIdOrderByScheduledDateAsc(applicationId);

        if (schedule.isEmpty()) {
            throw new ProgramLifecycleException("No disbursement schedule found.");
        }

        // 1. Identify latest PAID installment and its position
        int lastPaidIndex = -1;
        for (int i = 0; i < schedule.size(); i++) {
            if (schedule.get(i).getStatus() == DisbursementStatus.PAID) {
                lastPaidIndex = i;
            }
        }

        if (lastPaidIndex == -1) {
            throw new ProgramLifecycleException("No completed disbursements found. Initial payment required before reporting.");
        }

        Disbursement latestPaid = schedule.get(lastPaidIndex);
        LocalDate currentDate = LocalDate.now();
        LocalDate deadline;

        // 2. Logic: Intermediate vs. Final Installment
        if (lastPaidIndex < schedule.size() - 1) {
            // Rule: Deadline is 10 days BEFORE the next scheduled payment
            Disbursement nextDisbursement = schedule.get(lastPaidIndex + 1);
            deadline = nextDisbursement.getScheduledDate().minusDays(10);

            log.debug("Intermediate Window: Next payment on {}, Deadline set to {}",
                    nextDisbursement.getScheduledDate(), deadline);
        } else {
            // Rule: Final report due 30 days after the final payment actual date
            if (latestPaid.getActualDate() == null) {
                log.error("Data Integrity Error: Disbursement {} is PAID but lacks ActualDate", latestPaid.getDisbursementId());
                throw new ProgramLifecycleException("Payment record incomplete: Missing Actual Date.");
            }
            deadline = latestPaid.getActualDate().plusDays(30);
            log.debug("Final Window: Final payment was {}, Deadline set to {}",
                    latestPaid.getActualDate(), deadline);
        }

        // 3. Enforcement
        if (currentDate.isAfter(deadline)) {
            long daysOverdue = ChronoUnit.DAYS.between(deadline, currentDate);
            throw new ReportingWindowException("Reporting Window Closed. You are " + daysOverdue + " days late. Deadline was: " + deadline);
        }

        log.info("Window Validated for App: {}. Deadline: {}", applicationId, deadline);
    }
}