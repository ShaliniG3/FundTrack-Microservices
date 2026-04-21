package com.cts.fundtrack.disbursement.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.disbursement.client.ApplicationClient;
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
 * Primary implementation of the {@link ComplianceService} interface.
 * <p>
 * This service drives the compliance audit lifecycle within the FundTrack disbursement
 * platform. It validates and records officer audit decisions, builds compliance dashboards
 * for program administrators, detects delinquent applicants who have received payments
 * without submitting corresponding reports, and provides pre-disbursement compliance gate
 * checks for Finance Officers.
 * </p>
 * <p>
 * All read operations are annotated {@code @Transactional(readOnly = true)} for
 * performance optimisation. Mutating operations emit audit events via the
 * {@link com.cts.fundtrack.common.aspect.Auditable} AOP annotation.
 * </p>
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
    private final ApplicationClient applicationClient;
    private final HttpServletRequest request;

    /**
     * Extracts the authenticated user's UUID from the {@code X-User-Id} header
     * injected by the API Gateway filter chain.
     *
     * @return the current user's {@link UUID}, or {@code null} if the header is absent
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

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

        // Notification: Confirmation to the person who clicked "Audit"
        sendInternalNotification(getCurrentUserId(), report.getApplicationId(),
            "Audit Recorded: You have finalized the verdict for Report ID: " + report.getGrantReportId(),
            NotificationCategory.GENERAL);

        return "Audit complete. Report status synchronized to: " + report.getStatus();
    }

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

        return applicationIds.stream()
                .map(appId -> {
                    long paidInstallments = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);
                    List<GrantReport> reports = grantReportRepository.findByApplicationIdOrderBySubmittedDateDesc(appId);

                    // Only include applicants who have submitted at least one grant report
                    if (reports.isEmpty()) return null;

                    String complianceStatus = resolveStatus(reports, paidInstallments);
                    UUID latestReportId = reports.get(0).getGrantReportId();

                    String applicantName = "Unknown Applicant";
                    String programName = null;
                    try {
                        ApplicationMetadataDTO meta = applicationClient.getApplicationMetadata(appId);
                        if (meta != null) {
                            applicantName = meta.getApplicantName() != null ? meta.getApplicantName() : "Unknown Applicant";
                            programName = meta.getProgramName();
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch metadata for application {}: {}", appId, e.getMessage());
                    }

                    return ApplicantComplianceDTO.builder()
                            .applicationId(appId)
                            .applicantName(applicantName)
                            .programName(programName)
                            .latestReportStatus(complianceStatus)
                            .currentInstallment((int) paidInstallments)
                            .reportId(latestReportId)
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Derives a human-readable compliance status label for a single applicant based on
     * the relationship between their paid installments and submitted reports.
     *
     * @param reports          the applicant's grant reports ordered by submission date descending
     * @param paidInstallments the count of disbursement installments in {@code PAID} status
     * @return a descriptive status string such as {@code "READY_FOR_FIRST_DISBURSEMENT"},
     *         {@code "MISSING_REPORT_FOR_INSTALLMENT_N"}, or the name of the latest
     *         report's {@link com.cts.fundtrack.common.models.enums.GrantReportStatus}
     */
    private String resolveStatus(List<GrantReport> reports, long paidInstallments) {
        if (reports.isEmpty() && paidInstallments == 0) return "READY_FOR_FIRST_DISBURSEMENT";
        if (reports.size() < paidInstallments) return "MISSING_REPORT_FOR_INSTALLMENT_" + (reports.size() + 1);
        return reports.stream().findFirst().map(r -> r.getStatus().name()).orElse("NO_REPORTS_SUBMITTED");
    }

    /**
     * Converts a {@link ComplianceCheck} entity into a {@link ComplianceHistoryDTO}
     * for presentation in audit history views.
     *
     * @param check the {@link ComplianceCheck} entity to transform
     * @return a populated {@link ComplianceHistoryDTO} containing the check ID,
     *         report ID, application ID, result, and audit timestamp
     */
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
     * Dispatches an internal notification to a specific user after an audit action completes.
     * <p>
     * Notification failures are caught and logged as non-critical errors so that they
     * never roll back the enclosing audit transaction.
     * </p>
     *
     * @param userId   the UUID of the user to notify (typically the compliance officer)
     * @param appId    the UUID of the application providing context for the notification
     * @param message  the human-readable notification message body
     * @param category the {@link NotificationCategory} used to classify the alert in the UI
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        if (userId == null) {
            log.warn("Notification Aborted: Authenticated user context is null.");
            return;
        }
        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(userId)
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            notificationClient.sendNotification(notification);
            log.debug("System alert successfully transmitted to user: {}", userId);
        } catch (Throwable e) {
            log.error("Internal Notification Error: Feign Client failure: {}", e.getMessage());
        }
    }
}
