package com.cts.fundtrack.disbursement.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.common.exceptions.*;
import com.cts.fundtrack.common.models.enums.*;
import com.cts.fundtrack.disbursement.models.ComplianceCheck;
import com.cts.fundtrack.disbursement.models.GrantReport;
import com.cts.fundtrack.disbursement.repository.ComplianceCheckRepository;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;
import com.cts.fundtrack.disbursement.repository.GrantReportRepository;
import com.cts.fundtrack.disbursement.validation.ComplianceValidator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation managing post-disbursement compliance and grant reporting audits.
 * <p>
 * This service facilitates the official auditing of Grant Reports, synchronizes 
 * report statuses based on compliance verdicts, and dispatches system notifications 
 * to confirm administrative actions.
 * </p>
 *
 * @author FundTrack Development Team
 * @version 1.4
 * @since 2026-04-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceCheckRepository complianceCheckRepository;
    private final GrantReportRepository grantReportRepository;
    private final DisbursementRepository disbursementRepository;
    private final ComplianceValidator complianceValidator;
    private final NotificationClient notificationClient;
    private final HttpServletRequest request; // 👈 For current user context

    /**
     * Extracts the Unique Identifier of the currently authenticated Compliance Officer.
     * @return UUID of the logged-in user.
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Records an official audit result for a submitted Grant Report.
     * <p>
     * terminal states are enforced to ensure audit integrity. Successfully 
     * synchronized reports trigger a confirmation notification to the officer.
     * </p>
     *
     * @param dto Adjudication data for the specific report.
     * @return A status message confirming the synchronization outcome.
     * @throws GrantReportNotFoundException if the report ID is invalid.
     * @throws ReportLockedException if the report is in a terminal state (APPROVED/REJECTED).
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.APPLICATION)
    public String recordAudit(ComplianceCheckRequestDTO dto) {
        log.info("Initiating Audit Workflow | Report ID: {} | Verdict: {}", dto.getGrantReportId(), dto.getStatus());

        GrantReport report = grantReportRepository.findById(dto.getGrantReportId())
                .orElseThrow(() -> new GrantReportNotFoundException("Audit Error: Report not found for ID: " + dto.getGrantReportId()));

        if (report.getStatus() == GrantReportStatus.APPROVED || report.getStatus() == GrantReportStatus.REJECTED) {
            log.warn("Compliance Violation: Attempt to audit a terminal report (ID: {})", dto.getGrantReportId());
            throw new ReportLockedException("Audit Denied: This report has already reached a terminal state.");
        }

        ComplianceStatus auditResult;
        try {
            auditResult = ComplianceStatus.valueOf(dto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid status provided: " + dto.getStatus());
        }

        ComplianceCheck check = ComplianceCheck.builder()
                .grantReportId(report.getGrantReportId())
                .applicationId(report.getApplicationId())
                .complianceOfficerId(dto.getComplianceOfficerId())
                .type(dto.getType())
                .result(auditResult)
                .notes(dto.getComments())
                .date(Instant.now())
                .build();

        complianceCheckRepository.save(check);

        NotificationCategory alertCategory;
        String alertMessage;

        // Core Logic: Maintain original status synchronization
        if ("COMPLIANCE".equalsIgnoreCase(dto.getStatus())) {
            report.setStatus(GrantReportStatus.APPROVED);
            alertCategory = NotificationCategory.COMPLIANCE;
            alertMessage = "Success: Grant Report (ID: " + report.getGrantReportId() + ") verified as COMPLIANT.";
        } else if ("NON_COMPLIANT".equalsIgnoreCase(dto.getStatus())) {
            report.setStatus(GrantReportStatus.REJECTED);
            alertCategory = NotificationCategory.REJECTED;
            alertMessage = "Notice: Grant Report (ID: " + report.getGrantReportId() + ") marked as NON-COMPLIANT.";
        } else {
            throw new InvalidInputException("Invalid status: Operational modes are 'COMPLIANCE' or 'NON_COMPLIANT'.");
        }

        grantReportRepository.save(report);

        // 🚀 Notification: Confirmation to the person who clicked "Audit"
        sendInternalNotification(getCurrentUserId(), report.getApplicationId(), 
            "Audit Recorded: You have finalized the verdict for Report ID: " + report.getGrantReportId(), 
            NotificationCategory.GENERAL);

        return "Audit complete. Report status synchronized to: " + report.getStatus();
    }

    /**
     * Retrieves historical audit logs performed by a specific Compliance Officer.
     * @param complianceOfficerId The UUID of the officer.
     * @return List of historical compliance check data.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public List<ComplianceHistoryDTO> getComplianceHistoryByOfficer(UUID complianceOfficerId) {
        log.debug("Retrieving audit history for Compliance Officer: {}", complianceOfficerId);
        try {
            List<ComplianceCheck> checks = complianceCheckRepository.findByComplianceOfficerId(complianceOfficerId);
            return checks.stream().map(this::mapToHistoryDTO).toList();
        } catch (Exception e) {
            log.error("Forensic Retrieval Error: {}", e.getMessage());
            throw new DataExportException("Failed to retrieve audit history.");
        }
    }

    /**
     * Fetches a detailed summary of a specific Grant Report.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public GrantReportResponseDTO getGrantReportSummary(UUID reportId) {
        log.debug("Accessing summary for Report ID: {}", reportId);
        return grantReportRepository.findById(reportId)
                .map(report -> GrantReportResponseDTO.builder()
                        .grantReportId(report.getGrantReportId())
                        .applicationId(report.getApplicationId())
                        .scope(report.getScope())
                        .metrics(report.getMetrics())
                        .status(report.getStatus() != null ? report.getStatus().name() : "PENDING")
                        .documentUrl(report.getProofPath())
                        .build())
                .orElseThrow(() -> new GrantReportNotFoundException("Report not found for ID: " + reportId));
    }

    /**
     * Scans for applicants who are delinquent in their Grant Report submissions.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public List<ApplicantComplianceDTO> getNonSubmittingApplicants(UUID programId) {
        log.info("Executing Delinquency Scan | Program: {}", programId);
        List<UUID> applicationIds = disbursementRepository.findDistinctApplicationIdsByProgramId(programId);

        return applicationIds.stream()
                .filter(appId -> {
                    long paidCount = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);
                    long reportCount = grantReportRepository.countByApplicationId(appId);
                    return paidCount > reportCount;
                })
                .map(appId -> ApplicantComplianceDTO.builder()
                        .applicationId(appId)
                        .latestReportStatus("DELINQUENT_SUBMISSION_REQUIRED")
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isApplicantCompliant(UUID applicationId) {
        log.debug("Real-time compliance validation for Application: {}", applicationId);
        return complianceValidator.verifyCompliance(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicantComplianceDTO> getApplicantGrantReportingSummary(UUID programId) {
        log.debug("Aggregating reporting metrics for Program: {}", programId);
        List<UUID> applicationIds = disbursementRepository.findDistinctApplicationIdsByProgramId(programId);

        return applicationIds.stream().map(appId -> {
            long paidInstallments = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);
            List<GrantReport> reports = grantReportRepository.findByApplicationIdOrderBySubmittedDateDesc(appId);
            String complianceStatus = resolveStatus(reports, paidInstallments);

            return ApplicantComplianceDTO.builder()
                    .applicationId(appId)
                    .latestReportStatus(complianceStatus)
                    .currentInstallment((int) paidInstallments)
                    .build();
        }).toList();
    }

    private String resolveStatus(List<GrantReport> reports, long paidInstallments) {
        if (reports.isEmpty() && paidInstallments == 0) return "READY_FOR_FIRST_DISBURSEMENT";
        if (reports.size() < paidInstallments) return "MISSING_REPORT_FOR_INSTALLMENT_" + (reports.size() + 1);
        return reports.stream().findFirst().map(r -> r.getStatus().name()).orElse("NO_REPORTS_SUBMITTED");
    }

    private ComplianceHistoryDTO mapToHistoryDTO(ComplianceCheck check) {
        return ComplianceHistoryDTO.builder()
                .checkId(check.getCheckId())
                .grantReportId(check.getGrantReportId())
                .applicationId(check.getApplicationId())
                .result(check.getResult().name())
                .auditDate(check.getDate().toString())
                .build();
    }

    /**
     * Dispatcher for internal microservice notifications.
     * Targets the authenticated user to confirm administrative synchronization.
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        if (userId == null) {
            log.warn("Notification Aborted: Authenticated user context is null.");
            return;
        }
        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(userId) // 🚀 Confirmation to the doer
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            
            notificationClient.sendNotification(notification);
            log.debug("System alert successfully transmitted to user: {}", userId);
        } catch (Exception e) {
            log.error("Internal Notification Error: Feign Client failure: {}", e.getMessage());
        }
    }
}