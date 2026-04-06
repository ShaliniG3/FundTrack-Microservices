package com.fundtrack.modules.review_service.service;


import java.util.List;
import java.util.UUID;

import com.fundtrack.modules.application_service.dto.ApplicationResponseDTO;
import com.fundtrack.modules.review_service.dto.ReviewRequestDTO;
import com.fundtrack.modules.review_service.dto.ReviewerDashBoardDTO;

public interface ReviewService {
    List<ApplicationResponseDTO> getPendingApplications();
    void processReview(ReviewRequestDTO dto);
    void deleteReviewByApplicationId(UUID applicationId);
    ReviewerDashBoardDTO getReviewsByReviewer(UUID reviewerId);
    void patchReview(UUID applicationId, ReviewRequestDTO dto);
}