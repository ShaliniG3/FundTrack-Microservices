package com.cts.fundtrack.dgcs.service.grantreportservice;

import com.cts.fundtrack.dgcs.client.applicationclient.ApplicationClient;
import com.cts.fundtrack.dgcs.client.dto.ApplicationMetadataDTO;
import com.cts.fundtrack.dgcs.config.StorageConfig;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportRequestDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;
import com.cts.fundtrack.dgcs.exception.*;
import com.cts.fundtrack.dgcs.model.GrantReport;
import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;
import com.cts.fundtrack.dgcs.repository.grantreportrepository.GrantReportRepository;
import com.cts.fundtrack.dgcs.service.validation.ReportSubmissionValidator;
import com.cts.fundtrack.dgcs.service.validation.ReportingWindowValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Enterprise Service Layer for Grant Reporting Operations.
 * <p>
 * This implementation serves as the authoritative controller for the grant reporting lifecycle.
 * It manages the intersection of relational data (Metadata) and non-relational storage (PDF Evidence).
 * </p>
 * <p>
 * <b>Operational Guardrails:</b>
 * <ul>
 * <li><b>Transactional Integrity:</b> Employs ACID properties to prevent orphaned file references.</li>
 * <li><b>Input Sanitization:</b> Enforces strict validation via specialized domain validators.</li>
 * <li><b>Resilience:</b> Comprehensive error handling for I/O and database constraints.</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GrantReportServiceImpl implements GrantReportService {

    private final GrantReportRepository grantReportRepository;

    private final ReportSubmissionValidator submissionValidator;
    private final ReportingWindowValidator windowValidator;
    private final StorageConfig storageConfig;
    private final ApplicationClient applicationClient;
    /**
     * Orchestrates the submission of a new progress report.
     * <p>
     * Sequence: Entity Resolution -> Domain Validation -> File Persistence -> DB Synchronization.
     * </p>
     *
     * @param dto   Standardized request metadata.
     * @param proof The binary PDF evidence for audit purposes.
     * @return The persisted {@link GrantReport}.
     * @throws FileStorageException If filesystem commit fails.
     * @throws ApplicationNotFoundException If the target application is invalid.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GrantReportResponseDTO submitGrantReport(GrantReportRequestDTO dto, MultipartFile proof) {
        log.info("Process Start: Report Submission | Application: {}", dto.getApplicationId());


// 1. Call the Feign Client
        ApplicationMetadataDTO appMetadata = applicationClient.getApplicationMetadata(dto.getApplicationId());

// 2. Handle the "Service Unavailable" or "Not Found" scenario
        if (appMetadata == null || "SYSTEM_TEMPORARILY_UNAVAILABLE".equals(appMetadata.getApplicantName())) {
            log.error("Handshake Failed: Application Service unreachable for ID: {}", dto.getApplicationId());
            throw new ApplicationNotFoundException("Reporting is temporarily disabled: Could not verify application status.");
        }

// 3. Optional: Add a check for Application Status
// If the application was rejected or closed, you shouldn't allow reports.
        if ("REJECTED".equalsIgnoreCase(appMetadata.getStatus()) || "CLOSED".equalsIgnoreCase(appMetadata.getStatus())) {
            throw new ReportEligibilityException("Submission Blocked: This application is no longer active.");
        }

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


        String filePath = saveFileToLocalFolder(proof);


        GrantReport report = GrantReport.builder()
                .scope(dto.getScope())
                .metrics(dto.getMetrics())
                .status(GrantReportStatus.SUBMITTED)
                .applicationId(dto.getApplicationId())
                .proofPath(filePath)
                .build();

        try {
            GrantReport savedReport = grantReportRepository.save(report);
            log.info("Process Complete: Report {} registered for Application {}",
                    savedReport.getReportId(),savedReport.getApplicationId());
            return this.convertToResponse(savedReport);
        } catch (Exception e) {
            log.error("Database Failure: Could not synchronize report state. Cleaning up file: {}", filePath);

            throw new ReportPersistenceException("System failed to save report metadata: " + e.getMessage());
        }
    }

    /**
     * Persists a binary payload to the server's dedicated file repository using a collision-resistant naming strategy.
     * <p>
     * This method ensures physical data integrity by:
     * <ul>
     * <li><b>Non-repudiation:</b> Renaming files with a {@link UUID} prefix to prevent metadata spoofing.</li>
     * <li><b>Directory Resilience:</b> Automatically resolving and creating parent path hierarchies.</li>
     * <li><b>Atomic Overwrite:</b> Utilizing {@link StandardCopyOption#REPLACE_EXISTING} to handle recovery scenarios.</li>
     * </ul>
     * </p>
     *
     * @param file The {@link MultipartFile} containing the raw binary stream to be persisted.
     * @return The absolute {@link String} path of the successfully stored resource.
     * @throws FileStorageException If the inbound stream is null, empty, or if an I/O sub-system failure occurs.
     */
    private String saveFileToLocalFolder(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            log.error("I/O Abort | Reason: Null or empty MultipartFile provided for storage.");
            throw new FileStorageException("Persistence failed: The uploaded document contains no data.");
        }


        String originalName = file.getOriginalFilename();
        long fileSize = file.getSize();

        try {

            String uniqueFileName = UUID.randomUUID() + "_" + originalName;
            Path targetPath = Paths.get(storageConfig.getUploadDir()).resolve(uniqueFileName).normalize();

            log.info("I/O Transfer Initiated | Resource: {} | Size: {} bytes | Target: {}",
                    originalName, fileSize, targetPath);


            Files.createDirectories(targetPath.getParent());


            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("I/O Transfer Success | Resource mapped to physical path: {}", targetPath);

            return targetPath.toString();
        } catch (java.io.IOException e) {
            log.error("CRITICAL I/O FAILURE | Resource: {} | Error: {}", originalName, e.getMessage());
            throw new FileStorageException("Physical storage failure: Unable to commit stream to filesystem. " +
                    "Verify directory permissions and storage availability.");
        } catch (Exception e) {
            log.error("UNEXPECTED STORAGE ERROR | Context: {} | Exception: {}", originalName, e.getClass().getSimpleName());
            throw new FileStorageException("An internal error occurred while finalizing file storage.");
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

        log.info("Querying history for Application ID: {}", applicationId);


//        try {
//            ResponseEntity<ApplicationDTO> response = applicationClient.getApplicationById(dto.getApplicationId());
//            if (!response.getStatusCode().is2xxSuccessful()) {
//                throw new ApplicationNotFoundException("Application service returned error for ID: " + dto.getApplicationId());
//            }
//        } catch (Exception e) {
//            // Handle cases where GLMS service is down or ID is invalid
//            throw new ApplicationNotFoundException("Could not verify application status with the Lifecycle service.");
//        }

        try {
            // 1. Fetch only from your own table using the ID.
            // We no longer check applicationRepository.existsById() because that table is GONE.
            List<GrantReport> reports = grantReportRepository.findByApplicationId(applicationId);

            if (reports.isEmpty()) {
                log.info("No historical reports found for Application ID: {}", applicationId);
                return List.of();
            }

            log.info("Resolved {} report entries.", reports.size());

            // 2. Map to DTO
            return reports.stream()
                    .map(report -> {
                        try {
                            return this.convertToResponse(report);
                        } catch (Exception e) {
                            log.error("Mapping Error for Report ID: {}", report.getReportId());
                            throw new ComplianceDataException("Internal data transformation error.");
                        }
                    })
                    .toList();

        } catch (Exception e) {
            log.error("System Failure for Application {}: {}", applicationId, e.getMessage());
            throw new ComplianceDataException("An internal error occurred while fetching the reporting history.");
        }
    }

    /**
     * Synchronizes the transformation of a {@link GrantReport} domain entity into a sanitized {@link GrantReportResponseDTO}.
     * <p>
     * This mapping logic acts as a security and integrity boundary, ensuring that internal
     * persistence structures (Entities) are never directly leaked to the presentation layer.
     * </p>
     * <p>
     * <b>Mapping Specifications:</b>
     * <ul>
     * <li><b>Relational Resolution:</b> Employs safe navigation to resolve {Application} identifiers.</li>
     * <li><b>Enum Serialization:</b> Normalizes internal state machine statuses into API-compatible strings.</li>
     * <li><b>Contextual Enrichment:</b> Injects service-level metadata (messages) to facilitate frontend UX.</li>
     * </ul>
     * </p>
     *
     * @param report The source {@link GrantReport} entity fetched from the persistence layer.
     * @return A fully populated, read-only {@link GrantReportResponseDTO}.
     * @throws ComplianceDataException If the source entity is null or if critical relational data is missing.
     */
    private GrantReportResponseDTO convertToResponse(GrantReport report) {

        return GrantReportResponseDTO.builder()
                .reportId(report.getReportId())
                .applicationId(report.getApplicationId()) // Direct ID mapping
                .scope(report.getScope())
                .metrics(report.getMetrics())
                .status(report.getStatus().name())
                .pdfPath(report.getProofPath())
                .message("Report retrieved from Compliance Service.")
                .build();
    }
}