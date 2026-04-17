package com.cts.fundtrack.application.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ReviewRequestDTO;
import com.cts.fundtrack.common.dto.ReviewerDashBoardDTO;


/**
 * Service interface defining the business operations for the reviewer evaluation
 * workflow in the FundTrack grant funding system.
 *
 * <p>Implementations handle the intermediate stage of the application lifecycle
 * between initial submission and final approval. A reviewer scores an application,
 * records comments, and optionally attaches a recommendation. Processing a review
 * transitions the application from {@code SUBMITTED} to {@code UNDER_REVIEW},
 * making it visible to approvers. Reviewer activity is tracked for dashboard
 * and audit purposes.</p>
 *
 * @see ReviewServiceImpl
 */
public interface ReviewService {

    /**
     * Retrieves all grant applications currently in the {@code SUBMITTED} state,
     * forming the reviewer's pending work-queue.
     *
     * @return a list of {@link ApplicationResponseDTO} objects representing
     *         applications awaiting an initial reviewer evaluation; empty if
     *         none are pending
     */
    List<ApplicationResponseDTO> getPendingApplications();

    /**
     * Processes a new review for a grant application, or replaces an existing one.
     *
     * <p>The application must be in the {@code SUBMITTED} state. A valid score
     * (0–100) is required. On success, the application status is updated to
     * {@code UNDER_REVIEW}, an optional recommendation is persisted, and the
     * applicant receives a status-change notification.</p>
     *
     * @param dto the review payload containing the application ID, reviewer ID,
     *            numeric score, qualitative comments, and an optional recommendation
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotSubmittedException if
     *         the application is not in the {@code SUBMITTED} state
     * @throws com.cts.fundtrack.common.exceptions.InvalidReviewScoreException if the
     *         score is outside the 0–100 range
     */
    void processReview(ReviewRequestDTO dto);

    /**
     * Deletes the review and associated recommendation for a specific application,
     * reverting its status to {@code SUBMITTED}.
     *
     * <p>This is an administrative correction that allows a review to be retracted
     * and restarted. The applicant is notified that the review was retracted.</p>
     *
     * @param applicationId the UUID of the application whose review records are
     *                      to be deleted
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    void deleteReviewByApplicationId(UUID applicationId);

    /**
     * Retrieves the complete review history for a specific reviewer, for use in
     * dashboard display and workload auditing.
     *
     * @param reviewerId the UUID of the reviewer whose history is requested
     * @return a {@link ReviewerDashBoardDTO} containing the total review count
     *         and a list of individual review records; empty collections if the
     *         reviewer has no history
     */
    ReviewerDashBoardDTO getReviewsByReviewer(UUID reviewerId);

    /**
     * Partially updates an existing review and its associated recommendation for
     * a specific application.
     *
     * <p>Only the fields present in the request DTO are updated; absent fields
     * retain their current values. The applicant receives a notification
     * informing them of the updated feedback.</p>
     *
     * @param applicationId the UUID of the application whose review is to be patched
     * @param dto           the partial update payload; may contain a new score,
     *                      updated comments, and/or an updated recommendation
     * @throws RuntimeException if no review record exists for the given application ID
     * @throws com.cts.fundtrack.common.exceptions.InvalidReviewScoreException if a
     *         new score is provided but is outside the 0–100 range
     */
    void patchReview(UUID applicationId, ReviewRequestDTO dto);
}