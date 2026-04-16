package com.cts.fundtrack.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.mapper.ApplicationMapper;
import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.application.model.Recommendation;
import com.cts.fundtrack.application.model.Review;
import com.cts.fundtrack.application.repository.ApplicationRepository;
import com.cts.fundtrack.application.repository.RecommendationRepository;
import com.cts.fundtrack.application.repository.ReviewRepository;
import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.RecommendationDTO;
import com.cts.fundtrack.common.dto.ReviewDTO;
import com.cts.fundtrack.common.dto.ReviewRequestDTO;
import com.cts.fundtrack.common.dto.ReviewerDashBoardDTO;
import com.cts.fundtrack.common.exceptions.ApplicationNotFoundException;
import com.cts.fundtrack.common.exceptions.ApplicationNotSubmittedException;
import com.cts.fundtrack.common.exceptions.InvalidReviewScoreException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.ApplicationStatus;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.NotificationCategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing the application review lifecycle.
 * <p>
 * This service handles the scoring, qualitative comments, and internal 
 * recommendations provided by reviewers. It ensures that applications 
 * transition correctly from 'SUBMITTED' to 'UNDER_REVIEW' and provides 
 * real-time notifications to applicants regarding their review status.
 * </p>
 * * @author FundTrack Development Team
 * @version 1.2
 * @since 2026-04-16
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RecommendationRepository recommendationRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final NotificationClient notificationClient;

    /**
     * Retrieves a list of applications that have been submitted but not yet reviewed.
     * * @return A list of {@link ApplicationResponseDTO} objects in 'SUBMITTED' status.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION)
    public List<ApplicationResponseDTO> getPendingApplications() {
        log.debug("Fetching all pending applications for the reviewer work-queue.");
        return applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED)
                .map(applicationMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Processes a new review for an application, establishing its initial score and recommendation.
     * <p>
     * Triggers an 'UNDER_REVIEW' notification to the applicant.
     * </p>
     * * @param dto The review details including score and comments.
     * @throws ApplicationNotFoundException if the application ID is invalid.
     * @throws ApplicationNotSubmittedException if the application is not in a submittable state.
     * @throws InvalidReviewScoreException if the score is outside the 0-100 range.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.STATUS_CHANGE, entityName = EntityType.APPLICATION)
    public void processReview(ReviewRequestDTO dto) {
        log.info("Processing initial review for Application ID: {}", dto.getApplicationId());

        Application app = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(dto.getApplicationId()));

        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new ApplicationNotSubmittedException(dto.getApplicationId(), app.getStatus().name());
        }

        if (dto.getScore() == null || dto.getScore() < 0 || dto.getScore() > 100) {
            throw new InvalidReviewScoreException(dto.getScore());
        }

        UUID existingReviewId = reviewRepository.findByApplicationId(dto.getApplicationId())
                .map(Review::getReviewId)
                .orElse(null);

        Review review = Review.builder()
                .reviewId(existingReviewId)
                .applicationId(dto.getApplicationId())
                .reviewerId(dto.getReviewerId())
                .score(dto.getScore())
                .comments(dto.getComments())
                .date(LocalDate.now())
                .build();

        reviewRepository.save(review);

        if (dto.getRecommendation() != null) {
            handleRecommendation(dto.getRecommendation(), dto.getApplicationId(), dto.getReviewerId());
        }

        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(app);

        // 🚀 Notification: Status updated to Under Review
        sendInternalNotification(app.getApplicantId(), app.getApplicationId(), 
            "Great news! Your application is now officially Under Review by our technical committee.", 
            NotificationCategory.UNDER_REVIEW);
    }

    /**
     * Performs a partial update (patch) on an existing review.
     * * @param applicationId The target application.
     * @param dto The fields to be updated.
     * @throws RuntimeException if no review is found for the application.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.REVIEW)
    public void patchReview(UUID applicationId, ReviewRequestDTO dto) {
        log.info("Patching existing review for Application ID: {}", applicationId);
        
        Review review = reviewRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new RuntimeException("Review not found for Application: " + applicationId));

        if (dto.getScore() != null) {
            if (dto.getScore() < 0 || dto.getScore() > 100) {
                throw new InvalidReviewScoreException(dto.getScore());
            }
            review.setScore(dto.getScore());
        }

        if (dto.getComments() != null) {
            review.setComments(dto.getComments());
        }

        review.setDate(LocalDate.now());
        reviewRepository.save(review);

        if (dto.getRecommendation() != null) {
            Recommendation rec = recommendationRepository.findByApplicationId(applicationId)
                    .orElse(new Recommendation()); 

            rec.setApplicationId(applicationId);
            rec.setReviewerId(review.getReviewerId()); 

            if (dto.getRecommendation().getRecommendationStatus() != null) {
                rec.setDecision(dto.getRecommendation().getRecommendationStatus());
            }
            if (dto.getRecommendation().getJustification() != null) {
                rec.setNotes(dto.getRecommendation().getJustification());
            }
            rec.setDate(LocalDate.now());
            recommendationRepository.save(rec);
        }

        // 🚀 Notification: Inform user of an update in the review process
        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app != null) {
            sendInternalNotification(app.getApplicantId(), applicationId, 
                "Your application review has been updated with new evaluator feedback.", 
                NotificationCategory.APPLICATION);
        }
    }

    /**
     * Retrieves all reviews performed by a specific reviewer.
     * * @param reviewerId The ID of the reviewer.
     * @return A dashboard DTO containing the list of performed reviews.
     */
    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.REVIEW)
    public ReviewerDashBoardDTO getReviewsByReviewer(UUID reviewerId) {
        log.debug("Fetching review dashboard for Reviewer ID: {}", reviewerId);
        List<Review> reviews = reviewRepository.findByReviewerId(reviewerId);
        
        List<ReviewDTO> reviewDTOs = reviews.stream()
                .map(r -> ReviewDTO.builder()
                        .reviewId(r.getReviewId())
                        .applicationId(r.getApplicationId())
                        .score(r.getScore())
                        .comments(r.getComments())
                        .build())
                .collect(Collectors.toList());

        return ReviewerDashBoardDTO.builder()
                .count(reviewDTOs.size())
                .reviews(reviewDTOs)
                .build();
    }

    /**
     * Removes a review and its associated recommendation, reverting the application to 'SUBMITTED'.
     * * @param applicationId The ID of the application whose review is to be deleted.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.REVIEW)
    public void deleteReviewByApplicationId(UUID applicationId) {
        log.warn("Deleting review data for Application ID: {}. Reverting status to SUBMITTED.", applicationId);
        
        reviewRepository.deleteByApplicationId(applicationId);
        recommendationRepository.deleteByApplicationId(applicationId);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        app.setStatus(ApplicationStatus.SUBMITTED);
        applicationRepository.save(app);

        // 🚀 Notification: Revert Status Alert
        sendInternalNotification(app.getApplicantId(), applicationId, 
            "Alert: Your application review was retracted and the status has returned to Submitted.", 
            NotificationCategory.APPLICATION);
    }

    /**
     * Internal helper to persist recommendation details.
     */
    private void handleRecommendation(RecommendationDTO recDto, UUID appId, UUID reviewerId) {
        UUID existingRecId = recommendationRepository.findByApplicationId(appId)
                .map(Recommendation::getRecommendationId)
                .orElse(null);

        Recommendation rec = Recommendation.builder()
                .recommendationId(existingRecId)
                .applicationId(appId)
                .reviewerId(reviewerId)
                .decision(recDto.getRecommendationStatus())
                .notes(recDto.getJustification())
                .date(LocalDate.now())
                .build();

        recommendationRepository.save(rec);
    }

    /**
     * Dispatcher for internal system notifications.
     */
    private void sendInternalNotification(UUID userId, UUID appId, String message, NotificationCategory category) {
        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(userId)
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            notificationClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to dispatch internal notification for User {}: {}", userId, e.getMessage());
        }
    }
}