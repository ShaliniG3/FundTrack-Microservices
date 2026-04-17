package com.cts.fundtrack.disbursement.validation;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.models.enums.DisbursementStatus;
import com.cts.fundtrack.common.models.enums.GrantReportStatus;
import com.cts.fundtrack.disbursement.models.GrantReport;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;
import com.cts.fundtrack.disbursement.repository.GrantReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Validator component that enforces the core compliance rules for grant applications
 * within the FundTrack disbursement platform.
 * <p>
 * This component is the authoritative source for determining whether an applicant is
 * compliant and eligible to receive the next disbursement installment. It applies a
 * multi-stage rule chain:
 * <ol>
 *   <li><b>Structural Check:</b> A funding schedule must exist for the application.</li>
 *   <li><b>Gap Analysis:</b> The number of submitted reports must be at least equal
 *       to the number of paid installments (one report per paid disbursement).</li>
 *   <li><b>Policy Alignment:</b> The most recently submitted report must have been
 *       formally approved by a Compliance Officer.</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceValidator {

    private final DisbursementRepository disbursementRepository;
    private final GrantReportRepository grantReportRepository;

    /**
     * Evaluates whether a grant application satisfies all compliance obligations
     * required before the next disbursement can be released.
     * <p>
     * Applies the following rule chain in order:
     * <ol>
     *   <li>Rejects if no disbursement schedule exists for the application.</li>
     *   <li>Rejects if any paid installments exist but no reports have been submitted.</li>
     *   <li>Rejects if the report count is less than the paid installment count
     *       (gap analysis).</li>
     *   <li>Rejects if the most recent report is not in {@code APPROVED} status.</li>
     * </ol>
     * A special case allows zero reports when zero installments have been paid,
     * treating the application as compliant for the first disbursement.
     * </p>
     *
     * @param applicationId the UUID of the application to evaluate
     * @return {@code true} if the application is fully compliant and eligible for
     *         the next disbursement; {@code false} otherwise
     */
    public boolean verifyCompliance(UUID applicationId) {

        // 1. Structural Check
        long totalScheduled = disbursementRepository.countByApplicationId(applicationId);
        if (totalScheduled == 0) {
            log.warn("Compliance Reject | AppID: {} | Reason: No funding schedule found.", applicationId);
            return false;
        }

        // 2. Financial Progress: STRICTLY 'PAID' ONLY
        // This count drives the gap analysis.
        long paidCount = disbursementRepository.countByApplicationIdAndStatus(applicationId, DisbursementStatus.PAID);

        log.debug("Compliance Check | AppID: {} | PAID Installments: {}", applicationId, paidCount);

        // 3. Document History
        List<GrantReport> reports = grantReportRepository.findByApplicationIdOrderBySubmittedDateDesc(applicationId);

        // 4. Rule: Submission Integrity
        if (reports.isEmpty()) {
            // Special Case: If they haven't been PAID anything yet, 0 reports is technically "Compliant"
            // for the next step. If they HAVE been paid, 0 reports is a failure.
            if (paidCount > 0) {
                log.warn("Compliance Reject | AppID: {} | Reason: {} installments PAID but 0 reports found.",
                        applicationId, paidCount);
                return false;
            }
            return true;
        }

        // 5. Rule: Gap Analysis (One report for every PAID installment)
        if (reports.size() < paidCount) {
            log.warn("Compliance Reject | AppID: {} | Gap Detected: {} reports for {} PAID payments.",
                    applicationId, reports.size(), paidCount);
            return false;
        }

        // 6. Rule: Policy Alignment (Latest must be APPROVED)
        return reports.stream()
                .findFirst()
                .map(latest -> {
                    boolean isApproved = latest.getStatus() == GrantReportStatus.APPROVED;
                    if (!isApproved) {
                        log.warn("Compliance Check Failed | AppID: {} | Status: {}", applicationId, latest.getStatus());
                    }
                    return isApproved;
                })
                .orElse(false);
    }
}