package com.cts.fundtrack.application.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.application.service.ReviewService;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ReviewRequestDTO;
import com.cts.fundtrack.common.dto.ReviewerDashBoardDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Fetches all applications that are currently in 'SUBMITTED' status.
     * Accessible by Reviewers and Admins.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponseDTO>> getPendingApplications() {
        return ResponseEntity.ok(reviewService.getPendingApplications());
    }

    /**
     * Processes a new review or updates an existing one for an application.
     * Restricted to the 'REVIEWER' role.
     */
    @PostMapping("/process")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<String> processReview(@RequestBody ReviewRequestDTO reviewRequestDTO) {
        reviewService.processReview(reviewRequestDTO);
        return ResponseEntity.ok("Review processed successfully for Application ID: " + reviewRequestDTO.getApplicationId());
    }

    /**
     * Deletes the review and recommendation records for a specific application.
     * Typically an 'ADMIN' level override to revert state.
     */
    @DeleteMapping("/delete-application/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteReview(@PathVariable UUID applicationId) {
        reviewService.deleteReviewByApplicationId(applicationId);
        return ResponseEntity.ok("Review deleted and Application status reset for ID: " + applicationId);
    }

    /**
     * Fetches the total count and list of all reviews made by a specific reviewer.
     * Reviewers can view their history; Admins can audit.
     */
    @GetMapping("/reviewer/{reviewerId}")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<ReviewerDashBoardDTO> getReviewsByReviewerId(@PathVariable UUID reviewerId) {
        return ResponseEntity.ok(reviewService.getReviewsByReviewer(reviewerId));
    }

    /**
     * Partially updates an existing review and recommendation.
     * Restricted to the 'REVIEWER' role.
     */
    @PatchMapping("/update/{applicationId}")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<String> patchReview(
            @PathVariable UUID applicationId, 
            @RequestBody ReviewRequestDTO dto) {
            
        reviewService.patchReview(applicationId, dto);
        return ResponseEntity.ok("Review updated successfully for Application ID: " + applicationId);
    }
}