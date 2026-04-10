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

/**
 * Validator responsible for enforcing time-bound reporting deadlines.
 * <p>
 * This component calculates dynamic submission windows based on the progress
 * of the disbursement schedule, differentiating between intermediate and final reporting phases.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingWindowValidator {

    private final DisbursementRepository disbursementRepository;

    /**
     * Validates if the current date falls within the allowable reporting window for a grant.
     * <p>
     * Sequence: Schedule Resolution -> Installment Indexing -> Deadline Calculation -> Threshold Enforcement.
     * </p>
     *
     * @param applicationId The unique identifier for the grant application.
     * @throws ProgramLifecycleException If the schedule is missing or inconsistent.
     * @throws ReportingWindowException  If the current date exceeds the calculated deadline.
     */
    public void validateWindow(UUID applicationId) {

        log.info("Process Start: Dynamic Window Validation | Application: {}", applicationId);

        // 1. SCHEDULE RESOLUTION
        List<Disbursement> schedule = disbursementRepository
                .findByApplicationIdOrderByScheduledDateAsc(applicationId);

        if (schedule.isEmpty()) {
            log.warn("Validation Rejected: No disbursement schedule exists for Application: {}", applicationId);
            throw new ProgramLifecycleException("No disbursement schedule found.");
        }

        // 2. INSTALLMENT INDEXING
        // Locate the latest PAID installment to determine current progress
        int lastPaidIndex = -1;
        for (int i = 0; i < schedule.size(); i++) {
            if (DisbursementStatus.PAID.equals(schedule.get(i).getStatus())) {
                lastPaidIndex = i;
            }
        }

        if (lastPaidIndex == -1) {
            log.warn("Validation Rejected: Application {} has no completed payments.", applicationId);
            throw new ProgramLifecycleException("No completed disbursements found. Initial payment required before reporting.");
        }

        Disbursement latestPaid = schedule.get(lastPaidIndex);
        LocalDate currentDate = LocalDate.now();
        LocalDate deadline;

        // 3. DEADLINE CALCULATION
        if (lastPaidIndex < schedule.size() - 1) {
            // INTERMEDIATE WINDOW: Deadline is 10 days BEFORE the next scheduled payment
            Disbursement nextDisbursement = schedule.get(lastPaidIndex + 1);
            deadline = nextDisbursement.getScheduledDate().minusDays(10);

            log.debug("Logic Execution: Intermediate Window | Next Payment: {} | Calculated Deadline: {}",
                    nextDisbursement.getScheduledDate(), deadline);
        } else {
            // FINAL WINDOW: Final report due 30 days after the final payment's actual date
            if (latestPaid.getActualDate() == null) {
                log.error("Data Integrity Error: Disbursement {} is marked PAID but lacks an ActualDate.",
                        latestPaid.getDisbursementId());
                throw new ProgramLifecycleException("Payment record incomplete: Missing Actual Date.");
            }
            deadline = latestPaid.getActualDate().plusDays(30);

            log.debug("Logic Execution: Final Window | Last Payment Date: {} | Calculated Deadline: {}",
                    latestPaid.getActualDate(), deadline);
        }

        // 4. THRESHOLD ENFORCEMENT
        if (currentDate.isAfter(deadline)) {
            long daysOverdue = ChronoUnit.DAYS.between(deadline, currentDate);
            log.warn("Validation Rejected: Reporting window closed for Application {}. Overdue by {} days.",
                    applicationId, daysOverdue);

            throw new ReportingWindowException("Reporting Window Closed. You are " + daysOverdue +
                    " days late. Deadline was: " + deadline);
        }

        log.info("Process Complete: Reporting window is VALID for Application {}. Deadline: {}",
                applicationId, deadline);
    }
}