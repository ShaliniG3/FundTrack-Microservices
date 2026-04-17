package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.model.ApplicationValidation;

/**
 * Spring Data JPA repository for {@link ApplicationValidation} entities.
 *
 * <p>Provides persistence operations for the per-rule eligibility validation results
 * that are generated each time an application is submitted or updated. Supports
 * bulk deletion of stale results before re-running validation, and retrieval of
 * all results for a given application for display in the validation results endpoint.</p>
 */
@Repository
public interface ApplicationValidationRepository extends JpaRepository<ApplicationValidation, UUID> {

    /**
     * Deletes all validation result records associated with the specified application.
     *
     * <p>Called before re-running eligibility validation on an updated application
     * so that stale results do not persist alongside the new evaluation output.
     * Requires an active transaction; the {@code @Transactional} annotation ensures
     * one is present even when called without an enclosing service-layer transaction.</p>
     *
     * @param applicationId the UUID of the application whose validation records
     *                      should be deleted
     */
    @Transactional
    void deleteByApplication_ApplicationId(UUID applicationId);

    /**
     * Retrieves all validation result records associated with the specified application.
     *
     * @param applicationId the UUID of the application whose validation results
     *                      are requested
     * @return a list of {@link ApplicationValidation} entities; empty if no
     *         validation has been performed yet for the given application
     */
    List<ApplicationValidation> findByApplication_ApplicationId(UUID applicationId);
}