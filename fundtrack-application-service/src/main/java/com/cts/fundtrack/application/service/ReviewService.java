package com.cts.fundtrack.application.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ReviewRequestDTO;
import com.cts.fundtrack.common.dto.ReviewerDashBoardDTO;


public interface ReviewService {
    List<ApplicationResponseDTO> getPendingApplications();
    void processReview(ReviewRequestDTO dto);
    void deleteReviewByApplicationId(UUID applicationId);
    ReviewerDashBoardDTO getReviewsByReviewer(UUID reviewerId);
    void patchReview(UUID applicationId, ReviewRequestDTO dto);
}