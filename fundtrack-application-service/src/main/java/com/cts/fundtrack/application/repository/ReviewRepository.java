package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.model.Review;

/**
 * Spring Data JPA repository for {@link Review} entities.
 *
 * <p>Provides persistence operations for reviewer evaluations of grant applications.
 * Each application has at most one review record; the service layer performs an
 * upsert by reusing the existing {@code reviewId} when updating. Supports lookup
 * by application (for decision assembly and patch operations), deletion by
 * application (for admin-level revert), and lookup by reviewer (for the reviewer
 * dashboard and workload tracking).</p>
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /**
     * Retrieves the review record associated with a specific grant application.
     *
     * <p>Used when assembling the consolidated decision-detail view for approvers,
     * when patching an existing review, and when determining whether a review
     * already exists before processing a new one.</p>
     *
     * @param applicationId the UUID of the application whose review is requested
     * @return an {@link Optional} containing the {@link Review} if one exists,
     *         or empty if the application has not yet been reviewed
     */
    Optional<Review> findByApplicationId(UUID applicationId);

    /**
     * Deletes the review record associated with the specified application.
     *
     * <p>Called during the admin-level review deletion workflow to fully retract a
     * reviewer's evaluation and revert the application status to {@code SUBMITTED}.
     * Requires an active transaction; the {@code @Transactional} annotation ensures
     * one is present when this method is called directly.</p>
     *
     * @param applicationId the UUID of the application whose review is to be deleted
     */
    @Transactional
    void deleteByApplicationId(UUID applicationId);

    /**
     * Retrieves all reviews completed by a specific reviewer.
     *
     * <p>Used to populate the reviewer's historical dashboard and to support
     * workload tracking and performance auditing.</p>
     *
     * @param reviewerId the UUID of the reviewer whose review history is requested
     * @return a list of {@link Review} entities submitted by the reviewer; empty
     *         if none exist
     */
    List<Review> findByReviewerId(UUID reviewerId);
}