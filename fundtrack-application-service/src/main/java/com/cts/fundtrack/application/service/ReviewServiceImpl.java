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
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing the application review lifecycle.
 * Fully audited to ensure accountability in the scoring and recommendation process.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RecommendationRepository recommendationRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.APPLICATION) // 👈 Audit work-queue access
    public List<ApplicationResponseDTO> getPendingApplications() {
        return applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED)
                .map(applicationMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.STATUS_CHANGE, entityName = EntityType.APPLICATION) // 👈 Audit lifecycle transition
    public void processReview(ReviewRequestDTO dto) {
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
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.REVIEW) // 👈 Audit score/comment adjustments
    public void patchReview(UUID applicationId, ReviewRequestDTO dto) {
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
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(action = ActionType.READ, entityName = EntityType.REVIEW) // 👈 Audit reviewer history access
    public ReviewerDashBoardDTO getReviewsByReviewer(UUID reviewerId) {
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

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.REVIEW) // 👈 Audit removal of score data
    public void deleteReviewByApplicationId(UUID applicationId) {
        reviewRepository.deleteByApplicationId(applicationId);
        recommendationRepository.deleteByApplicationId(applicationId);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        app.setStatus(ApplicationStatus.SUBMITTED);
        applicationRepository.save(app);
    }

    /**
     * Internal helper - No @Auditable here because internal calls bypass the Spring Proxy.
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
}