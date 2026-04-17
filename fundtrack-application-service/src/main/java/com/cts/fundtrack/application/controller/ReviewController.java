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

/**
 * REST controller that exposes review workflow endpoints for the reviewer stage
 * of the FundTrack grant funding lifecycle.
 *
 * <p>Base path: {@code /api/v1/reviews}</p>
 *
 * <p>Reviewers are responsible for the initial technical evaluation of submitted
 * grant applications. They score applications, add comments, and issue a
 * recommendation (e.g., "Recommended" or "Not Recommended") before the application
 * progresses to the approver for a final funding decision.</p>
 *
 * <p>Roles used in this controller:
 * <ul>
 *   <li>{@code REVIEWER} — processes and updates reviews</li>
 *   <li>{@code ADMIN} — has full access including deletion/revert operations</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Retrieves all grant applications currently in the {@code SUBMITTED} state,
     * forming the reviewer's pending work-queue.
     *
     * <p>Only applications that have passed automated eligibility validation and
     * are awaiting human review appear in this list.</p>
     *
     * @return a {@link ResponseEntity} with HTTP 200 and a list of
     *         {@link ApplicationResponseDTO} objects representing pending applications
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<List<ApplicationResponseDTO>> getPendingApplications() {
        return ResponseEntity.ok(reviewService.getPendingApplications());
    }

    /**
     * Submits a new review — or replaces an existing one — for a grant application.
     *
     * <p>The application must be in the {@code SUBMITTED} state. Upon success the
     * application status is updated to {@code UNDER_REVIEW}, the review score and
     * comments are persisted, an optional recommendation record is created, and
     * the applicant receives a status-change notification.</p>
     *
     * @param reviewRequestDTO the review payload containing the application ID,
     *                         reviewer ID, numeric score (0–100), comments, and
     *                         an optional {@link com.cts.fundtrack.common.dto.RecommendationDTO}
     * @return a {@link ResponseEntity} with HTTP 200 and a confirmation message
     *         indicating the application ID that was reviewed
     */
    @PostMapping("/process")
    @PreAuthorize("hasRole('REVIEWER')")
    public ResponseEntity<String> processReview(@RequestBody ReviewRequestDTO reviewRequestDTO) {
        reviewService.processReview(reviewRequestDTO);
        return ResponseEntity.ok("Review processed successfully for Application ID: " + reviewRequestDTO.getApplicationId());
    }

    /**
     * Deletes the review and associated recommendation for a specific application,
     * reverting its status to {@code SUBMITTED}.
     *
     * <p>This is an administrative correction operation that allows a review to be
     * retracted and restarted. The applicant is notified that the review was
     * retracted and their application status has returned to Submitted.</p>
     *
     * @param applicationId the UUID of the application whose review records are to
     *                      be deleted
     * @return a {@link ResponseEntity} with HTTP 200 and a confirmation message
     */
    @DeleteMapping("/delete-application/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteReview(@PathVariable UUID applicationId) {
        reviewService.deleteReviewByApplicationId(applicationId);
        return ResponseEntity.ok("Review deleted and Application status reset for ID: " + applicationId);
    }

    /**
     * Retrieves the complete review history for a specific reviewer, supporting
     * workload tracking and performance audits.
     *
     * <p>A reviewer may view their own history; an admin may query any reviewer's
     * history.</p>
     *
     * @param reviewerId the UUID of the reviewer whose history is requested
     * @return a {@link ResponseEntity} with HTTP 200 and a
     *         {@link ReviewerDashBoardDTO} containing the total review count and
     *         a list of individual {@link com.cts.fundtrack.common.dto.ReviewDTO} entries
     */
    @GetMapping("/reviewer/{reviewerId}")
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public ResponseEntity<ReviewerDashBoardDTO> getReviewsByReviewerId(@PathVariable UUID reviewerId) {
        return ResponseEntity.ok(reviewService.getReviewsByReviewer(reviewerId));
    }

    /**
     * Partially updates an existing review and its associated recommendation for
     * a specific application.
     *
     * <p>Only the fields provided in the request body are updated; absent fields
     * retain their existing values. The applicant receives a notification
     * informing them that their review has been updated with new evaluator
     * feedback.</p>
     *
     * @param applicationId the UUID of the application whose review is to be patched
     * @param dto           the partial update payload; may contain a new score,
     *                      updated comments, and/or an updated recommendation
     * @return a {@link ResponseEntity} with HTTP 200 and a confirmation message
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