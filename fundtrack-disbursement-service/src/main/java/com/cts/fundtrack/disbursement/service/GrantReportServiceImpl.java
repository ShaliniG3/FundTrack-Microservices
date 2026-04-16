package com.cts.fundtrack.disbursement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.GrantReportRequestDTO;
import com.cts.fundtrack.common.dto.GrantReportResponseDTO;
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.exceptions.ComplianceDataException;
import com.cts.fundtrack.common.exceptions.ReportPersistenceException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.GrantReportStatus;
import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.disbursement.client.ApplicationClient;
import com.cts.fundtrack.disbursement.models.GrantReport;
import com.cts.fundtrack.disbursement.repository.GrantReportRepository;
import com.cts.fundtrack.disbursement.validation.ReportSubmissionValidator;
import com.cts.fundtrack.disbursement.validation.ReportingWindowValidator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing the submission and retrieval of Grant Progress Reports.
 * <p>
 * This service acts as the primary intake for financial evidence and progress metrics 
 * submitted by grant recipients. It enforces business rules regarding submission 
 * windows and eligibility, persists binary proof via a dedicated storage service, 
 * and notifies users of successful registrations.
 * </p>
 *
 * @author FundTrack Development Team
 * @version 1.5
 * @since 2026-04-16
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
    private final NotificationClient notificationClient;
    private final HttpServletRequest request;

    /**
     * Extracts the Unique Identifier of the currently authenticated user from request headers.
     * @return UUID of the logged-in user.
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Processes the submission of a new progress report and its associated binary proof.
     * <p>
     * Validates the application's eligibility and reporting window, stores the file 
     * in the persistent storage layer, and saves the metadata. A confirmation 
     * notification is sent to the submitter upon completion.
     * </p>
     *
     * @param dto   Metadata for the report (scope, metrics, etc.).
     * @param proof The binary file (PDF/Image) provided as evidence.
     * @return {@link GrantReportResponseDTO} containing the registered report details.
     * @throws ReportPersistenceException if metadata synchronization fails.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = ActionType.CREATE, entityName = EntityType.APPLICATION)
    public GrantReportResponseDTO submitGrantReport(GrantReportRequestDTO dto, MultipartFile proof) {
        log.info("Processing Grant Report Submission | Application ID: {}", dto.getApplicationId());

        // 1. Business Logic & Validation
        try {
            log.debug("Validation: Checking submission eligibility for AppID: {}", dto.getApplicationId());
            submissionValidator.validate(dto.getApplicationId());

            log.debug("Validation: Verifying reporting window status.");
            windowValidator.validate(dto.getApplicationId());
        } catch (Exception e) {
            log.warn("Validation Rejected: Submission for Application {} failed business rules: {}", 
                     dto.getApplicationId(), e.getMessage());
            throw e;
        }

        // 2. File Persistence
        String filePath = fileStorageService.store(proof);

        // 3. Database Synchronization
        GrantReport report = GrantReport.builder()
                .scope(dto.getScope())
                .metrics(dto.getMetrics())
                .status(GrantReportStatus.SUBMITTED)
                .applicationId(dto.getApplicationId())
                .proofPath(filePath)
                .build();

        try {
            GrantReport savedReport = grantReportRepository.save(report);
            log.info("Submission Successful: Grant report {} registered for AppID {}.", 
                     savedReport.getGrantReportId(), dto.getApplicationId());
            
            // 🚀 Notification: Transactional Confirmation to the currently logged-in user
            sendInternalNotification(getCurrentUserId(), dto.getApplicationId(), 
                "Submission Success: Your progress report for Application " + dto.getApplicationId() + " has been received and is awaiting audit.", 
                NotificationCategory.COMPLIANCE);

            return this.convertToResponse(savedReport);
        } catch (Exception e) {
            log.error("Persistence Failure: Metadata sync failed for file {}. Triggering rollback.", filePath);
            throw new ReportPersistenceException("System failed to save report metadata.");
        }
    }

    /**
     * Retrieves the reporting history associated with a specific application.
     *
     * @param applicationId The unique identifier of the application.
     * @return A list of historically submitted reports.
     * @throws ComplianceDataException if data retrieval fails.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public List<GrantReportResponseDTO> getMyGrantReports(UUID applicationId) {
        log.info("Retrieving reporting history for Application ID: {}", applicationId);

        try {
            List<GrantReport> reports = grantReportRepository.findByApplicationId(applicationId);

            if (reports.isEmpty()) {
                log.info("History Search: No report entries found for Application: {}", applicationId);
                return List.of();
            }

            return reports.stream()
                    .map(this::convertToResponse)
                    .toList();

        } catch (Exception e) {
            log.error("Retrieval Error: Failed to fetch report history for Application: {}. Error: {}", 
                      applicationId, e.getMessage());
            throw new ComplianceDataException("Internal error occurred while fetching the reporting history.");
        }
    }

    /**
     * Converts a domain entity to a response DTO.
     */
    private GrantReportResponseDTO convertToResponse(GrantReport report) {
        if (report == null) {
            throw new ComplianceDataException("Internal Error: Entity to DTO transformation received null input.");
        }
        return GrantReportResponseDTO.builder()
                .grantReportId(report.getGrantReportId())
                .applicationId(report.getApplicationId())
                .scope(report.getScope())
                .metrics(report.getMetrics())
                .status(report.getStatus().name())
                .documentUrl(report.getProofPath())
                .acknowledgment("System Confirmation: Report successfully registered in Disbursement Service.")
                .build();
    }

    /**
     * Dispatches notifications to the central Notification Microservice.
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        if (userId == null) {
            log.warn("Notification Aborted: Authenticated user context not available.");
            return;
        }
        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(userId) // 🚀 Targets the person who clicked "Submit"
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            
            notificationClient.sendNotification(notification);
            log.debug("Confirmation alert successfully queued for user: {}", userId);
        } catch (Exception e) {
            log.error("Feign Communication Error: Unable to transmit notification to user {}: {}", userId, e.getMessage());
        }
    }
}