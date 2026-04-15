package com.cts.fundtrack.disbursement.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.dto.*;
import com.cts.fundtrack.common.exceptions.*;
import com.cts.fundtrack.common.models.enums.*;
import com.cts.fundtrack.disbursement.client.ApplicationClient;
import com.cts.fundtrack.disbursement.models.ComplianceCheck;
import com.cts.fundtrack.disbursement.models.GrantReport;
import com.cts.fundtrack.disbursement.repository.ComplianceCheckRepository;
import com.cts.fundtrack.disbursement.repository.DisbursementRepository;
import com.cts.fundtrack.disbursement.repository.GrantReportRepository;
import com.cts.fundtrack.disbursement.validation.ComplianceValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceCheckRepository complianceCheckRepository;
    private final GrantReportRepository grantReportRepository;
    private final DisbursementRepository disbursementRepository;
    private final ApplicationClient applicationClient;
    private final ComplianceValidator complianceValidator;

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.APPLICATION) // 👈 Audit: Status transition
    public String recordAudit(ComplianceCheckRequestDTO dto) {
        log.info("Audit Workflow Initiated | Report ID: {} | Verdict: {}", dto.getGrantReportId(), dto.getStatus());

        GrantReport report = grantReportRepository.findById(dto.getGrantReportId())
                .orElseThrow(() -> new GrantReportNotFoundException("Report not found with ID: " + dto.getGrantReportId()));

        if (report.getStatus() == GrantReportStatus.APPROVED || report.getStatus() == GrantReportStatus.REJECTED) {
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

        if ("COMPLIANCE".equalsIgnoreCase(dto.getStatus())) {
            report.setStatus(GrantReportStatus.APPROVED);
        } else if ("NON_COMPLIANT".equalsIgnoreCase(dto.getStatus())) {
            report.setStatus(GrantReportStatus.REJECTED);
        } else {
            throw new InvalidInputException("Invalid status: Use 'APPROVED' or 'REJECTED' only.");
        }

        grantReportRepository.save(report);
        return "Audit complete. Report status synchronized to: " + report.getStatus();
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION) // 👈 Audit: Historical lookup
    public List<ComplianceHistoryDTO> getComplianceHistoryByOfficer(UUID complianceOfficerId) {
        log.info("Compliance Audit Retrieval | Target Officer ID: {}", complianceOfficerId);
        try {
            List<ComplianceCheck> checks = complianceCheckRepository.findByComplianceOfficerId(complianceOfficerId);
            return checks.stream().map(this::mapToHistoryDTO).toList();
        } catch (Exception e) {
            throw new DataExportException("Failed to retrieve audit history.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION) // 👈 Audit: Snapshot access
    public GrantReportResponseDTO getGrantReportSummary(UUID reportId) {
        log.info("Forensic Access | Snapshot retrieval for Report ID: {}", reportId);
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
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION) // 👈 Audit: Delinquency scan
    public List<ApplicantComplianceDTO> getNonSubmittingApplicants(UUID programId) {
        log.info("Compliance Scan Initiated | Target Program: {}", programId);
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
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION) // 👈 Audit: Eligibility check
    public boolean isApplicantCompliant(UUID applicationId) {
        log.info("Compliance Validation | Real-time status check for AppID: {}", applicationId);
        return complianceValidator.verifyCompliance(applicationId);
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION) // 👈 Audit: Dashboard access
    public List<ApplicantComplianceDTO> getApplicantGrantReportingSummary(UUID programId) {
        log.info("Administrative Audit | Initiating summary for Program: {}", programId);
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
}