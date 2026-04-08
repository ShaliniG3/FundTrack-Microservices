package com.cts.fundtrack.dgcs.service.validation;

import com.cts.fundtrack.dgcs.model.GrantReport;
import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;
import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;
import com.cts.fundtrack.dgcs.repository.grantreportrepository.GrantReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplianceValidator {

    private final DisbursementRepository disbursementRepository;
    private final GrantReportRepository grantReportRepository;

    /**
     * Microservice logic: Validates compliance using raw UUIDs.
     * Enforces the rule: Released installments (PAID) must match approved reporting.
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