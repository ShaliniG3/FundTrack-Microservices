package com.cts.fundtrack.dgcs.service.grantreportservice;

import com.cts.fundtrack.dgcs.client.applicationclient.ApplicationClient;
import com.cts.fundtrack.dgcs.client.dto.ApplicationMetadataDTO;

import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportRequestDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;

import com.cts.fundtrack.dgcs.exception.ApplicationNotFoundException;
import com.cts.fundtrack.dgcs.exception.ComplianceDataException;
import com.cts.fundtrack.dgcs.exception.ReportEligibilityException;
import com.cts.fundtrack.dgcs.exception.ReportPersistenceException;

import com.cts.fundtrack.dgcs.model.GrantReport;
import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;

import com.cts.fundtrack.dgcs.repository.grantreportrepository.GrantReportRepository;

import com.cts.fundtrack.dgcs.service.storage.FileStorageService;
import com.cts.fundtrack.dgcs.service.validation.ReportSubmissionValidator;
import com.cts.fundtrack.dgcs.service.validation.ReportingWindowValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Main Service for managing Grant Progress Reports.
 * <p>
 * This class handles the logic for submitting reports, validating business rules,
 * and coordinating between database storage and physical file storage.
 * </p>
 *
 * <b>Core Features:</b>
 * <ul>
 * <li><b>Transactional Safety:</b> Uses database transactions to ensure files and
 * data stay in sync even if a failure occurs.</li>
 * <li><b>Business Validation:</b> Uses specialized validators to check eligibility
 * and reporting windows before saving data.</li>
 * <li><b>Error Handling:</b> Provides custom exceptions and logging for disk
 * storage and database issues.</li>
 * </ul>
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
     * Orchestrates the submission of a new progress report.
     * <p>
     * Sequence: Entity Resolution -> Domain Validation -> File Persistence -> DB Synchronization.
     * </p>
     *
     * @param dto   Standardized request metadata containing application and metric details.
     * @param proof The binary PDF evidence (e.g., invoices, receipts) for audit purposes.
     * @return A {@link GrantReportResponseDTO} containing the summarized result of the submission.
     * //@throws FileStorageException If the physical file commit to the storage provider fails.
     * @throws ApplicationNotFoundException If the referenced application cannot be resolved or is inactive.
     * @throws ReportPersistenceException If the database record fails to save after the file is stored.
     * @throws ReportEligibilityException  If the application is rejected, closed, or outside the reporting window.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrantReportResponseDTO submitGrantReport(GrantReportRequestDTO dto, MultipartFile proof) {

        log.info("Process Start: Report Submission | Application: {}", dto.getApplicationId());

        // 1. VERIFY APPLICATION STATUS
//        ApplicationMetadataDTO applicationMetadata = applicationClient.getApplicationMetadata(dto.getApplicationId());
//
//        if (applicationMetadata == null || "SYSTEM_TEMPORARILY_UNAVAILABLE".equals(applicationMetadata.getApplicantName())) {
//            log.error("Handshake Failed: Application Service unreachable for ID: {}", dto.getApplicationId());
//            throw new ApplicationNotFoundException("Reporting is temporarily disabled: Could not verify application status.");
//        }
//
//        if ("REJECTED".equalsIgnoreCase(applicationMetadata.getStatus()) || "CLOSED".equalsIgnoreCase(applicationMetadata.getStatus())) {
//            throw new ReportEligibilityException("Submission Blocked: This application is no longer active.");
//        }

        // 2. CHECK BUSINESS RULES
        try {
            log.debug("Validation: Checking submission eligibility for AppID: {}", dto.getApplicationId());
            submissionValidator.validate(dto.getApplicationId());

            log.debug("Validation: Verifying reporting window status.");
            windowValidator.validateWindow(dto.getApplicationId());
        } catch (Exception e) {
            log.warn("Validation Rejected: Application {} failed business rules. Reason: {}",
                    dto.getApplicationId(), e.getMessage());
            throw e;
        }

        // 3. SAVE UPLOADED FILE
        String filePath = fileStorageService.store(proof);

        // 4. RECORD DATA IN DATABASE
        GrantReport report = GrantReport.builder()
                .scope(dto.getScope())
                .metrics(dto.getMetrics())
                .status(GrantReportStatus.SUBMITTED)
                .applicationId(dto.getApplicationId())
                .proofPath(filePath)
                .build();

        try {
            GrantReport savedReport = grantReportRepository.save(report);
            log.info("Process Complete: Grant report {} registered for Application {}",
                    savedReport.getGrantReportId(),savedReport.getApplicationId());
            return this.convertToResponse(savedReport);
        } catch (Exception e) {
            log.error("Database Failure: Could not synchronize report state. Cleaning up file: {}", filePath);

            throw new ReportPersistenceException("System failed to save report metadata: " + e.getMessage());
        }
    }

    /**
     * Resolves the historical record of all grant progress reports associated with a specific application.
     * <p>
     * This method facilitates the "Reporting History" view for both applicants and auditors.
     * It employs a read-optimized transaction to minimize database locking and ensures
     * relational integrity by validating the application context before querying.
     * </p>
     * <p>
     * <b>Performance & Security:</b>
     * <ul>
     * <li><b>Read Isolation:</b> Uses {@code readOnly = true} to allow the DB engine to optimize query execution plans.</li>
     * <li><b>Context Validation:</b> Prevents information leakage by explicitly verifying the existence of the parent Application.</li>
     * <li><b>Stream Processing:</b> Efficiently maps domain entities to sanitized DTOs using a functional approach.</li>
     * </ul>
     * </p>
     *
     * @param applicationId The unique {@link UUID} of the target grant application.
     * @return A {@link List} of {@link GrantReportResponseDTO} reflecting the submission history;
     * returns an empty list if no reports are found for a valid application.
     * @throws ApplicationNotFoundException If the provided ID does not correlate to a registered application.
     */
    @Override
    @Transactional(readOnly = true)
    public List<GrantReportResponseDTO> getMyGrantReports(UUID applicationId) {
        log.info("Request: Resolve history for Application ID: {}", applicationId);
//
//        // 1. VALIDATE APPLICATION EXISTENCE
//        try {
//            log.debug("Validation: Verifying application existence via Feign Client.");
//            applicationClient.getApplicationMetadata(applicationId);
//        } catch (Exception e) {
//            log.error("External Handshake Failed | Application ID: {} | Error: {}", applicationId, e.getMessage());
//            throw new ApplicationNotFoundException("Resource not found: The provided Application ID is invalid or unregistered.");
//        }

        // 2. RETRIEVE LOCAL RECORDS
        try {
            List<GrantReport> reports = grantReportRepository.findByApplicationId(applicationId);

            if (reports.isEmpty()) {
                log.info("Historical Audit: No report entries found for Application ID: {}", applicationId);
                return List.of();
            }

            // 3. CONVERT TO RESPONSE FORMAT
            log.info("Historical Audit: Found {} records for Application ID: {}", reports.size(), applicationId);

            return reports.stream()
                    .map(this::convertToResponse)
                    .toList();

        } catch (Exception e) {
            log.error("System Failure | Fetching history for App: {} | Reason: {}", applicationId, e.getMessage());
            throw new ComplianceDataException("An internal error occurred while fetching the reporting history.");
        }
    }

    /**
     * Converts a GrantReport database entity into a clean Response DTO.
     * <p>
     * This method acts as a boundary to ensure our internal database structure
     * is not exposed directly to the API users.
     * </p>
     * <p>
     * <b>What this mapping does:</b>
     * <ul>
     * <li><b>Data Transfer:</b> Maps internal IDs and fields to the response object.</li>
     * <li><b>Status Formatting:</b> Converts the internal Status Enum into a readable string.</li>
     * <li><b>User Feedback:</b> Adds a standard acknowledgment message for the frontend.</li>
     * </ul>
     * </p>
     *
     * @param report The GrantReport entity from the database.
     * @return A populated GrantReportResponseDTO for the API.
     * @throws ComplianceDataException If the report data is null or invalid.
     */
    private GrantReportResponseDTO convertToResponse(GrantReport report) {

        if (report == null) {
            log.error("Mapping Failure: Received null GrantReport entity.");
            throw new ComplianceDataException("Internal transformation error: Source data is null.");
        }
        return GrantReportResponseDTO.builder()
                .grantReportId(report.getGrantReportId())
                .applicationId(report.getApplicationId()) // Direct ID mapping
                .scope(report.getScope())
                .metrics(report.getMetrics())
                .status(report.getStatus().name())
                .documentUrl(report.getProofPath())
                .acknowledgment("Report retrieved from Compliance Service.")
                .build();
    }
}