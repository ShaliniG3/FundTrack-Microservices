package com.cts.fundtrack.application.repository;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cts.fundtrack.application.model.Recommendation;



/**
 * Spring Data JPA repository for {@link Recommendation} entities.
 *
 * <p>Provides persistence operations for reviewer recommendations attached to grant
 * applications. Each application has at most one recommendation record; upsert
 * behaviour is achieved in the service layer by finding and overwriting an existing
 * record when present. Supports deletion during review retraction and lookup during
 * decision assembly.</p>
 */
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    /**
     * Deletes the recommendation record associated with the specified application.
     *
     * <p>Called during the admin-level review deletion workflow to remove the
     * recommendation alongside the corresponding review record, fully reverting
     * the application to its pre-review state.</p>
     *
     * @param applicationId the UUID of the application whose recommendation is
     *                      to be deleted
     */
    void deleteByApplicationId(UUID applicationId);

    /**
     * Retrieves the recommendation record for a specific grant application.
     *
     * <p>Used when assembling the consolidated decision-detail view for approvers
     * and when performing a patch update to an existing recommendation.</p>
     *
     * @param applicationId the UUID of the application whose recommendation is
     *                      requested
     * @return an {@link Optional} containing the {@link Recommendation} if one
     *         exists, or empty if no recommendation has been recorded for this
     *         application
     */
    Optional<Recommendation> findByApplicationId(UUID applicationId);
}