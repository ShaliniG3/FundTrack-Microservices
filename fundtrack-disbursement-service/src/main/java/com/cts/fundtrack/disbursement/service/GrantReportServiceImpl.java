package com.cts.fundtrack.disbursement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cts.fundtrack.common.aspect.Auditable; // 👈 IMPORT YOUR COMMON ANNOTATION
import com.cts.fundtrack.common.dto.GrantReportRequestDTO;
import com.cts.fundtrack.common.dto.GrantReportResponseDTO;
import com.cts.fundtrack.common.exceptions.ComplianceDataException;
import com.cts.fundtrack.common.exceptions.ReportPersistenceException;
import com.cts.fundtrack.common.models.enums.ActionType; // 👈 IMPORT ENUMS
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.GrantReportStatus;
import com.cts.fundtrack.disbursement.client.ApplicationClient;
import com.cts.fundtrack.disbursement.models.GrantReport;
import com.cts.fundtrack.disbursement.repository.GrantReportRepository;
import com.cts.fundtrack.disbursement.validation.ReportSubmissionValidator;
import com.cts.fundtrack.disbursement.validation.ReportingWindowValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Main Service for managing Grant Progress Reports.
 * Fully audited to track the submission of financial evidence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GrantReportServiceImpl implements GrantReportService {

    private final ApplicationClient applicationClient;
    private final GrantReportRepository grantReportRepository;
    private final ReportSubmissionValidator submissionValidator;
    private final ReportingWindowValidator windowValidator;
    private final FileStorageService fileStorageService;

    /**
     * Records the submission of a new report and its binary proof.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = ActionType.CREATE, entityName = EntityType.APPLICATION) // 👈 AUDIT ENABLED
    public GrantReportResponseDTO submitGrantReport(GrantReportRequestDTO dto, MultipartFile proof) {

        log.info("Process Start: Report Submission | Application: {}", dto.getApplicationId());

        // 1. Business Logic & Validation
        try {
            log.debug("Validation: Checking submission eligibility for AppID: {}", dto.getApplicationId());
            submissionValidator.validate(dto.getApplicationId());

            log.debug("Validation: Verifying reporting window status.");
            windowValidator.validate(dto.getApplicationId());
        } catch (Exception e) {
            log.warn("Validation Rejected: Application {} failed business rules.", dto.getApplicationId());
            throw e;
        }

        // 2. File Persistence (Audit trail will record the file path in the Response DTO)
        String filePath = fileStorageService.store(proof);

        // 3. Database Sync
        GrantReport report = GrantReport.builder()
                .scope(dto.getScope())
                .metrics(dto.getMetrics())
                .status(GrantReportStatus.SUBMITTED)
                .applicationId(dto.getApplicationId())
                .proofPath(filePath)
                .build();

        try {
            GrantReport savedReport = grantReportRepository.save(report);
            log.info("Process Complete: Grant report {} registered.", savedReport.getGrantReportId());
            return this.convertToResponse(savedReport);
        } catch (Exception e) {
            log.error("Database Failure: Cleaning up file: {}", filePath);
            throw new ReportPersistenceException("System failed to save report metadata.");
        }
    }

    /**
     * Records when a user or auditor views the reporting history.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION) // 👈 AUDIT ENABLED
    public List<GrantReportResponseDTO> getMyGrantReports(UUID applicationId) {
        log.info("Request: Resolve history for Application ID: {}", applicationId);

        try {
            List<GrantReport> reports = grantReportRepository.findByApplicationId(applicationId);

            if (reports.isEmpty()) {
                log.info("Historical Audit: No report entries found for ID: {}", applicationId);
                return List.of();
            }

            return reports.stream()
                    .map(this::convertToResponse)
                    .toList();

        } catch (Exception e) {
            log.error("System Failure | Fetching history for App: {}", applicationId);
            throw new ComplianceDataException("Internal error occurred while fetching the reporting history.");
        }
    }

    /**
     * Internal utility - Note: AOP won't trigger for private methods.
     */
    private GrantReportResponseDTO convertToResponse(GrantReport report) {
        if (report == null) {
            throw new ComplianceDataException("Internal transformation error: Source data is null.");
        }
        return GrantReportResponseDTO.builder()
                .grantReportId(report.getGrantReportId())
                .applicationId(report.getApplicationId())
                .scope(report.getScope())
                .metrics(report.getMetrics())
                .status(report.getStatus().name())
                .documentUrl(report.getProofPath())
                .acknowledgment("Report successfully registered in Disbursement Service.")
                .build();
    }
}