package com.cts.fundtrack.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.mapper.ApplicationMapper;
import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.application.model.Decision;
import com.cts.fundtrack.application.model.Document;
import com.cts.fundtrack.application.repository.ApplicationRepository;
import com.cts.fundtrack.application.repository.DecisionRepository;
import com.cts.fundtrack.application.repository.DocumentRepository;
import com.cts.fundtrack.application.repository.RecommendationRepository;
import com.cts.fundtrack.application.repository.ReviewRepository;
import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.ApplicationDecisionDetailsDTO;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ApproverDashBoardDTO;
import com.cts.fundtrack.common.dto.DecisionDTO;
import com.cts.fundtrack.common.dto.DecisionRequestDTO;
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.RecommendationDTO;
import com.cts.fundtrack.common.dto.ReviewDTO;
import com.cts.fundtrack.common.exceptions.ApplicationNotFoundException;
import com.cts.fundtrack.common.exceptions.ApplicationNotReadyForDecisionException;
import com.cts.fundtrack.common.exceptions.InvalidDecisionTypeException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.ApplicationStatus;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.common.models.enums.VerificationStatus;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Primary implementation of {@link DecisionService}, providing the business logic
 * for the approver decision workflow in the FundTrack grant funding system.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Retrieves applications in the {@code UNDER_REVIEW} state for the approver
 *       console work-queue.</li>
 *   <li>Assembles consolidated decision-detail views combining application data,
 *       reviewer scores, and recommendations from multiple repositories.</li>
 *   <li>Records final {@code APPROVED} or {@code REJECTED} decisions and propagates
 *       the outcome to the application status and all associated document verification
 *       statuses.</li>
 *   <li>Supports amendment and revocation of existing decisions, reverting application
 *       state accordingly.</li>
 *   <li>Dispatches workflow and confirmation notifications to applicants and approvers
 *       via the Notification Service.</li>
 *   <li>Records auditable actions through the {@link com.cts.fundtrack.common.aspect.Auditable}
 *       AOP annotation.</li>
 * </ul>
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DecisionServiceImpl implements DecisionService {

    private final DecisionRepository decisionRepository;
    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationMapper applicationMapper;
    private final ReviewRepository reviewRepository;
    private final RecommendationRepository recommendationRepository;
    private final NotificationClient notificationClient;
    private final HttpServletRequest request;

    /**
     * Extracts the authenticated user's UUID from the {@code X-User-Id} HTTP
     * header injected by the API Gateway.
     *
     * @return the current user's {@link UUID}, or {@code null} if the header
     *         is absent
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public List<ApplicationResponseDTO> getApplicationsUnderReview() {
        log.debug("Accessing global work-queue for applications in UNDER_REVIEW state.");
        return applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.UNDER_REVIEW)
                .map(applicationMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public ApplicationDecisionDetailsDTO getApplicationById(UUID applicationId) {
        log.debug("Retrieving consolidated decision data for Application ID: {}", applicationId);
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        ApplicationResponseDTO appDto = applicationMapper.toResponseDTO(app);

        ReviewDTO reviewDto = reviewRepository.findByApplicationId(applicationId)
                .map(rev -> ReviewDTO.builder()
                        .reviewId(rev.getReviewId())
                        .applicationId(rev.getApplicationId())
                        .score(rev.getScore())
                        .comments(rev.getComments())
                        .build())
                .orElse(null);

        RecommendationDTO recDto = recommendationRepository.findByApplicationId(applicationId)
                .map(rec -> RecommendationDTO.builder()
                        .recommendationId(rec.getRecommendationId())
                        .recommendationStatus(rec.getDecision())
                        .justification(rec.getNotes())
                        .build())
                .orElse(null);

        return ApplicationDecisionDetailsDTO.builder()
                .applicationDetails(appDto)
                .review(reviewDto)
                .recommendation(recDto)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.DECISION)
    public ApproverDashBoardDTO getDecisionsByApprover(UUID approverId) {
        log.debug("Generating historical decision dashboard for Approver: {}", approverId);
        List<Decision> decisions = decisionRepository.findByApproverId(approverId);

        List<DecisionDTO> decisionDTOs = decisions.stream()
                .map(d -> DecisionDTO.builder()
                        .decisionId(d.getDecisionId())
                        .applicationId(d.getApplicationId())
                        .approverId(d.getApproverId())
                        .decision(d.getDecision())
                        .notes(d.getNotes())
                        .date(d.getDate())
                        .build())
                .collect(Collectors.toList());

        return ApproverDashBoardDTO.builder()
                .count(decisionDTOs.size())
                .decisions(decisionDTOs)
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.STATUS_CHANGE, entityName = EntityType.APPLICATION)
    public void processDecision(DecisionRequestDTO dto) {
        log.info("Processing terminal decision for Application ID: {} | Outcome: {}", dto.getApplicationId(), dto.getDecision());

        Application app = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(dto.getApplicationId()));

        if (app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationNotReadyForDecisionException(dto.getApplicationId(), app.getStatus().name());
        }

        ApplicationStatus finalStatus;
        try {
            finalStatus = ApplicationStatus.valueOf(dto.getDecision().toUpperCase());
            if (finalStatus != ApplicationStatus.APPROVED && finalStatus != ApplicationStatus.REJECTED) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidDecisionTypeException(dto.getDecision());
        }

        Decision decision = Decision.builder()
                .applicationId(dto.getApplicationId())
                .approverId(dto.getApproverId())
                .decision(dto.getDecision())
                .notes(dto.getNotes())
                .date(LocalDate.now())
                .build();
        decisionRepository.save(decision);

        app.setStatus(finalStatus);
        applicationRepository.save(app);

        VerificationStatus docStatus = ApplicationStatus.REJECTED.equals(finalStatus)
                ? VerificationStatus.DOCUMENT_REJECTED
                : VerificationStatus.DOCUMENT_APPROVED;

        List<Document> documents = documentRepository.findByApplication_ApplicationId(dto.getApplicationId());
        for (Document doc : documents) {
            doc.setVerificationStatus(docStatus);
            documentRepository.save(doc);
        }

        // Workflow Notification (Target: Applicant)
        NotificationCategory workflowCat = (finalStatus == ApplicationStatus.APPROVED)
                ? NotificationCategory.APPROVAL
                : NotificationCategory.REJECTED;

        sendInternalNotification(app.getApplicantId(), app.getApplicationId(),
            "Important Update: A final decision has been reached regarding your application. Result: " + finalStatus, workflowCat);

        // Transactional Confirmation (Target: Logged-in Approver)
        sendInternalNotification(getCurrentUserId(), app.getApplicationId(),
            "Success: You have finalized the adjudication for Application ID " + app.getApplicationId() + " as " + finalStatus,
            NotificationCategory.GENERAL);
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.DECISION)
    public void updateDecision(UUID applicationId, DecisionRequestDTO dto) {
        Decision decision = decisionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Decision not found for application: " + applicationId));

        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(dto.getDecision().toUpperCase());
            if (newStatus != ApplicationStatus.APPROVED && newStatus != ApplicationStatus.REJECTED) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidDecisionTypeException(dto.getDecision());
        }

        decision.setDecision(dto.getDecision());
        decision.setNotes(dto.getNotes());
        decision.setDate(LocalDate.now());
        decisionRepository.save(decision);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        app.setStatus(newStatus);
        applicationRepository.save(app);

        VerificationStatus docStatus = ApplicationStatus.REJECTED.equals(newStatus)
                ? VerificationStatus.DOCUMENT_REJECTED
                : VerificationStatus.DOCUMENT_APPROVED;

        List<Document> documents = documentRepository.findByApplication_ApplicationId(applicationId);
        for (Document doc : documents) {
            doc.setVerificationStatus(docStatus);
            documentRepository.save(doc);
        }
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.DECISION)
    public void deleteDecisionByApplicationId(UUID applicationId) {
        log.warn("REVOCATION: Removing terminal decision for Application ID: {}", applicationId);

        Decision decision = decisionRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Decision record missing for ID: " + applicationId));

        decisionRepository.delete(decision);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(app);

        List<Document> documents = documentRepository.findByApplication_ApplicationId(applicationId);
        for (Document doc : documents) {
            doc.setVerificationStatus(VerificationStatus.SUBMITTED);
            documentRepository.save(doc);
        }

        // Workflow Alert to Applicant
        sendInternalNotification(app.getApplicantId(), applicationId,
            "Alert: Your application status has been reverted to Under Review for administrative correction.",
            NotificationCategory.UNDER_REVIEW);

        // Confirmation to Staff
        sendInternalNotification(getCurrentUserId(), applicationId,
            "Confirmation: You have successfully revoked the terminal decision for App ID: " + applicationId,
            NotificationCategory.GENERAL);
    }

    /**
     * Dispatches an in-system notification to the specified user via the Notification
     * Service Feign client.
     *
     * <p>The call is fire-and-forget: any exception from the Notification Service is
     * caught and logged so that notification failures never disrupt decision business
     * logic. If {@code userId} is {@code null} the call is aborted with a warning log.</p>
     *
     * @param userId   the UUID of the notification recipient; if {@code null} the
     *                 notification is silently skipped
     * @param appId    the UUID of the related application to include in the notification
     *                 payload; may be {@code null}
     * @param message  the human-readable notification message body
     * @param category the {@link com.cts.fundtrack.common.models.enums.NotificationCategory}
     *                 classifying the event type
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        if (userId == null) {
            log.warn("Notification aborted: Recipient user context is null.");
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
        } catch (Exception e) {
            log.error("Failed to transmit notification to user {}: {}", userId, e.getMessage());
        }
    }
}
