package com.cts.fundtrack.disbursement.validation;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.exceptions.ReportEligibilityException;
import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;
import com.cts.fundtrack.disbursement.repository.GrantReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Business logic validator responsible for enforcing the Disbursement-to-Report ratio.
 * <p>
 * This component acts as a logical gatekeeper within the report submission workflow.
 * Its primary purpose is to ensure that reports are strictly reactive—meaning an
 * applicant can only file a progress report if a corresponding disbursement
 * has been successfully processed and approved.
 * </p>
 * <p>
 * <b>Integrity Rules:</b>
 * <ul>
 * <li><b>Cardinality Enforcement:</b> Maintains a 1:1 ratio between funded installments and submitted documentation.</li>
 * <li><b>Sequence Validation:</b> Prevents premature reporting for future installments that have not yet been disbursed.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingWindowValidator {

    private final DisbursementRepository disbursementRepository;
    private final GrantReportRepository grantReportRepository;

    /**
     * Validates that the applicant is within an open reporting window before accepting
     * a new grant report submission.
     * <p>
     * Enforces two rules:
     * <ol>
     *   <li>At least one disbursement installment must have been paid — applicants
     *       cannot submit reports for money they have not yet received.</li>
     *   <li>The total number of existing reports (in any status) must be less than
     *       the number of paid installments — preventing duplicate or premature
     *       submissions while a previous report is still under compliance review.</li>
     * </ol>
     * </p>
     *
     * @param applicationId the UUID of the application being submitted for reporting
     * @throws ReportEligibilityException if no payments have been disbursed yet, or if
     *         the applicant has already submitted reports for all paid installments
     */
    public void validate(UUID applicationId) {
        log.debug("Assessing reporting eligibility for AppID: {}", applicationId);

        // 1. How many payments have actually been finalized?
        long completedPayments = disbursementRepository.countByApplicationIdAndStatus(
                applicationId, DisbursementStatus.PAID);

        // 2. How many reports are currently in the system?
        // We count SUBMITTED, UNDER_REVIEW, and APPROVED to ensure the user doesn't
        // spam reports while one is still being checked by compliance.
        long totalReportsInProcess = grantReportRepository.countByApplicationId(applicationId);

        log.info("AppID: {} | Paid Installments: {} | Reports Found: {}",
                applicationId, completedPayments, totalReportsInProcess);

        // RULE: You cannot report on a disbursement that hasn't happened yet.
        if (completedPayments == 0) {
            throw new ReportEligibilityException("Submission Blocked: No payments have been disbursed yet.");
        }

        // RULE: You can only have one report per disbursement.
        // If reports >= payments, they have either already reported on the current money
        // or they are trying to report on money they haven't received.
        if (totalReportsInProcess >= completedPayments) {
            throw new ReportEligibilityException("Submission Blocked: Reporting requirements fulfilled for current disbursements.");
        }
    }
}