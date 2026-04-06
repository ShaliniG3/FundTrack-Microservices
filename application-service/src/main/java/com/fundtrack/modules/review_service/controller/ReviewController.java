package com.fundtrack.modules.review_service.controller;

import com.fundtrack.modules.application_service.dto.ApplicationResponseDTO;
import com.fundtrack.modules.review_service.dto.ReviewRequestDTO;
import com.fundtrack.modules.review_service.dto.ReviewerDashBoardDTO;
import com.fundtrack.modules.review_service.service.ReviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Fetches all applications that are currently in 'SUBMITTED' status.
     * Uses the getPendingApplications logic from the service. [cite: 8, 46]
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ApplicationResponseDTO>> getPendingApplications() {
        List<ApplicationResponseDTO> pendingApps = reviewService.getPendingApplications();
        return ResponseEntity.ok(pendingApps);
    }

    /**
     * Processes a new review or updates an existing one for an application.
     * This endpoint handles the score, status update, and recommendation mapping. [cite: 52, 54, 55]
     */
    @PostMapping("/process")
    public ResponseEntity<String> processReview(@RequestBody ReviewRequestDTO reviewRequestDTO) {
        reviewService.processReview(reviewRequestDTO);
        return ResponseEntity.ok("Review processed successfully for Application ID: " + reviewRequestDTO.getApplicationId());
    }

    /**
     * Deletes the review and recommendation records for a specific application.
     * Reverts the application status to 'SUBMITTED'.
     */
    @DeleteMapping("/delete-application/{applicationId}")
    public ResponseEntity<String> deleteReview(@PathVariable UUID applicationId) {
        reviewService.deleteReviewByApplicationId(applicationId);
        return ResponseEntity.ok("Review deleted and Application status reset for ID: " + applicationId);
    }



    /**
     * Fetches the total count and list of all reviews made by a specific reviewer.
     */
    @GetMapping("/reviewer/{reviewerId}")
    public ResponseEntity<ReviewerDashBoardDTO> getReviewsByReviewerId(@PathVariable UUID reviewerId) {
        ReviewerDashBoardDTO dashboardData = reviewService.getReviewsByReviewer(reviewerId);
        return ResponseEntity.ok(dashboardData);
    }

    /**
     * Partially updates an existing review and recommendation.
     * Omitted fields in the JSON body will retain their previous values in the database.
     */
    @PatchMapping("/update/{applicationId}")
    public ResponseEntity<String> patchReview(
            @PathVariable UUID applicationId, 
            @RequestBody ReviewRequestDTO dto) {
            
        reviewService.patchReview(applicationId, dto);
        return ResponseEntity.ok("Review updated successfully for Application ID: " + applicationId);
    }
}