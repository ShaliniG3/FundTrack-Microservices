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
 * Primary implementation of the {@link GrantReportService} interface.
 * <p>
 * This service manages the full submission lifecycle of grant progress reports within
 * the FundTrack disbursement platform. It enforces two business-rule validators before
 * accepting a submission — {@link ReportSubmissionValidator} (general eligibility) and
 * {@link ReportingWindowValidator} (disbursement-to-report ratio) — then delegates
 * binary file persistence to {@link FileStorageService} and stores structured metadata
 * via JPA.
 * </p>
 * <p>
 * All mutating operations participate in a full transaction with rollback on any
 * exception, ensuring file storage and database writes remain consistent. Audit events
 * are emitted via the {@link com.cts.fundtrack.common.aspect.Auditable} AOP annotation.
 * </p>
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
    @Transactional(rollbackFor = Exception.class)
    @Auditable(action = ActionType.CREATE, entityName = EntityType.APPLICATION)
    public GrantReportResponseDTO submitGrantReport(GrantReportRequestDTO dto, MultipartFile proof) {
        log.info("Processing Grant Report Submission | Application ID: {}", dto.getApplicationId());

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

        String filePath = fileStorageService.store(proof);

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

            // Notification: Transactional Confirmation to the currently logged-in user
            sendInternalNotification(getCurrentUserId(), dto.getApplicationId(),
                "Submission Success: Your progress report for Application " + dto.getApplicationId() + " has been received and is awaiting audit.",
                NotificationCategory.COMPLIANCE);

            return this.convertToResponse(savedReport);
        } catch (Exception e) {
            log.error("Persistence Failure: Metadata sync failed for file {}. Triggering rollback.", filePath);
            throw new ReportPersistenceException("System failed to save report metadata.");
        }
    }

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
     * Maps a persisted {@link GrantReport} entity to a {@link GrantReportResponseDTO}
     * suitable for returning to API consumers.
     * <p>
     * Falls back to the string {@code "PENDING"} for the status field if the entity's
     * status is {@code null}, and appends a system acknowledgment message.
     * </p>
     *
     * @param report the {@link GrantReport} entity to convert; must not be {@code null}
     * @return a fully populated {@link GrantReportResponseDTO}
     * @throws com.cts.fundtrack.common.exceptions.ComplianceDataException if
     *         {@code report} is {@code null}
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
                .status(report.getStatus() != null ? report.getStatus().name() : "PENDING")
                .documentUrl(report.getProofPath())
                .acknowledgment("System Confirmation: Report successfully registered in Disbursement Service.")
                .build();
    }

    /**
     * Dispatches a transactional confirmation notification to the applicant after a
     * successful report submission.
     * <p>
     * Notification failures are caught and logged as non-critical errors so that they
     * never roll back the enclosing report submission transaction.
     * </p>
     *
     * @param userId   the UUID of the user to notify (typically the submitting applicant)
     * @param appId    the UUID of the application providing context for the notification
     * @param message  the human-readable notification message body
     * @param category the {@link NotificationCategory} used to classify the alert in the UI
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        if (userId == null) {
            log.warn("Notification Aborted: Authenticated user context not available.");
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
            log.debug("Confirmation alert successfully queued for user: {}", userId);
        } catch (Exception e) {
            log.error("Feign Communication Error: Unable to transmit notification to user {}: {}", userId, e.getMessage());
        }
    }
}
