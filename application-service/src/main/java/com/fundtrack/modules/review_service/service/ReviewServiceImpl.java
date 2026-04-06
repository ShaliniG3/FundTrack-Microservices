package com.fundtrack.modules.review_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fundtrack.exceptions.ApplicationNotFoundException;
import com.fundtrack.exceptions.ApplicationNotSubmittedException;
import com.fundtrack.exceptions.InvalidReviewScoreException;
import com.fundtrack.modules.application_service.dto.ApplicationResponseDTO;
import com.fundtrack.modules.application_service.mappers.ApplicationMapper;
import com.fundtrack.modules.application_service.models.Application;
import com.fundtrack.modules.application_service.models.enums.ApplicationStatus;
import com.fundtrack.modules.application_service.repository.ApplicationRepository;
import com.fundtrack.modules.review_service.dto.RecommendationDTO;
import com.fundtrack.modules.review_service.dto.ReviewRequestDTO;
import com.fundtrack.modules.review_service.dto.ReviewerDashBoardDTO;
import com.fundtrack.modules.review_service.models.Recommendation;
import com.fundtrack.modules.review_service.models.Review;
import com.fundtrack.modules.review_service.repositories.RecommendationRepository;
import com.fundtrack.modules.review_service.repositories.ReviewRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RecommendationRepository recommendationRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    public List<ApplicationResponseDTO> getPendingApplications() {
        return applicationRepository.findAll().stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SUBMITTED)
                .map(applicationMapper::toResponseDTO) // <--- Convert to DTO here
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processReview(ReviewRequestDTO dto) {
        // 1. Fetch Application & Verify State (Must be SUBMITTED or UNDER_REVIEW)
        Application app = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new ApplicationNotFoundException(dto.getApplicationId()));

        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new ApplicationNotSubmittedException(dto.getApplicationId(), app.getStatus().name());
        }

        // 2. Validate Score range (0-100)
        if (dto.getScore() == null || dto.getScore() < 0 || dto.getScore() > 100) {
            throw new InvalidReviewScoreException(dto.getScore());
        }

        // 3. Builder Pattern for Review (Handles create and update)
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

        // 4. Handle Recommendation Logic
        if (dto.getRecommendation() != null) {
            handleRecommendation(dto.getRecommendation(), dto.getApplicationId(), dto.getReviewerId());
        }

        // 5. Update Application State to UNDER_REVIEW
        // The reviewer suggests a decision, but the system marks it as in-progress.
        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        applicationRepository.save(app);

        log.info("Review & Recommendation successfully saved for Application ID: {}", dto.getApplicationId());
    }

    @Override
    @Transactional
    public void deleteReviewByApplicationId(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new ApplicationNotFoundException(applicationId);
        }

        reviewRepository.deleteByApplicationId(applicationId);
        recommendationRepository.deleteByApplicationId(applicationId);

        // Reset Application status back to SUBMITTED if review is removed
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        app.setStatus(ApplicationStatus.SUBMITTED);
        applicationRepository.save(app);
    }



    @Override
    public ReviewerDashBoardDTO getReviewsByReviewer(UUID reviewerId) {
        List<Review> reviews = reviewRepository.findByReviewerId(reviewerId);
        return new ReviewerDashBoardDTO(reviews.size(), reviews);
    }

    @Override
    @Transactional
    public void patchReview(UUID applicationId, ReviewRequestDTO dto) {
        // Fetch existing review
        Review review = reviewRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId)); // Or a custom ReviewNotFoundException

        // PARTIAL UPDATE: Only change fields that are NOT null in the incoming DTO
        if (dto.getScore() != null) {
            if (dto.getScore() < 0 || dto.getScore() > 100) {
                throw new InvalidReviewScoreException(dto.getScore());
            }
            review.setScore(dto.getScore());
        }

        if (dto.getComments() != null) {
            review.setComments(dto.getComments());
        }

        review.setDate(LocalDate.now()); // Always update the modified date
        reviewRepository.save(review);

        // PARTIAL UPDATE FOR RECOMMENDATION
        if (dto.getRecommendation() != null) {
            Recommendation rec = recommendationRepository.findByApplicationId(applicationId)
                    .orElse(new Recommendation()); 

            // Ensure IDs are set if this is accidentally creating a new one
            rec.setApplicationId(applicationId);
            rec.setReviewerId(review.getReviewerId()); 

            if (dto.getRecommendation().getDecision() != null) {
                rec.setDecision(dto.getRecommendation().getDecision());
            }
            if (dto.getRecommendation().getNotes() != null) {
                rec.setNotes(dto.getRecommendation().getNotes());
            }
            rec.setDate(LocalDate.now());
            recommendationRepository.save(rec);
        }
        
        log.info("Partial update applied to Review for Application ID: {}", applicationId);
    }

    /**
     * Internal helper to persist the reviewer's suggested decision.
     */
    private void handleRecommendation(RecommendationDTO recDto, UUID appId, UUID reviewerId) {
        UUID existingRecId = recommendationRepository.findByApplicationId(appId)
                .map(Recommendation::getRecommendationId)
                .orElse(null);

        Recommendation rec = Recommendation.builder()
                .recommendationId(existingRecId)
                .applicationId(appId)
                .reviewerId(reviewerId)
                .decision(recDto.getDecision())
                .notes(recDto.getNotes())
                .date(LocalDate.now())
                .build();

        recommendationRepository.save(rec);
    }
}