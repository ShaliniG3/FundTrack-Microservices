package com.cts.fundtrack.dgcs.service.validation;

import com.cts.fundtrack.dgcs.exception.ReportEligibilityException;
import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;
import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;
import com.cts.fundtrack.dgcs.repository.grantreportrepository.GrantReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
public class ReportSubmissionValidator {

    private final DisbursementRepository disbursementRepository;
    private final GrantReportRepository grantReportRepository;

    public void validate(UUID applicationId) {
        log.debug("Cardinality Validation Initiated | Assessing reporting eligibility for AppID: {}", applicationId);

        // FIX: Use countByApplicationIdAndStatus (remove the underscore relationship)
        long completedPayments = disbursementRepository.countByApplicationIdAndStatus(
                applicationId, DisbursementStatus.PAID);

        // FIX: Use countByApplicationIdAndStatus (remove the underscore relationship)
        long existingReports = grantReportRepository.countByApplicationIdAndStatus(
                applicationId,
                GrantReportStatus.SUBMITTED
        );

        log.info("Eligibility Audit | AppID: {} | Approved Payments: {} | Existing Reports: {}",
                applicationId, completedPayments, existingReports);

        // The Invariant Rule: Reports must never exceed the number of payments.
        if (existingReports >= completedPayments) {
            log.warn("Validation Failure: Report Saturation Reached | AppID: {} | Ratio: {}/{}",
                    applicationId, existingReports, completedPayments);

            throw new ReportEligibilityException("Submission Blocked: You have already fulfilled reporting requirements " +
                    "for your current disbursements.");
        }

        log.info("Cardinality Validation Passed | AppID: {} | Ratio: {}/{}",
                applicationId, existingReports, completedPayments);
    }
}