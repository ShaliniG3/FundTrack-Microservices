package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.common.models.enums.ApplicationStatus;

/**
 * Spring Data JPA repository for {@link Application} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository} as well
 * as custom derived query methods supporting the core workflows of the Application
 * Service — including duplicate detection at submission time and applicant-scoped
 * application lookups for the "My Applications" dashboard.</p>
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    /**
     * Retrieves all applications currently in the specified lifecycle status.
     *
     * <p>Used internally when filtering applications by status without loading
     * the full dataset in memory.</p>
     *
     * @param status the {@link ApplicationStatus} to filter by
     * @return a list of matching {@link Application} entities; empty if none found
     */
    List<Application> findAllByStatus(ApplicationStatus status);

    /**
     * Checks whether an application already exists for the given applicant and program
     * combination, enforcing the one-application-per-program constraint.
     *
     * @param applicantId the UUID of the applicant
     * @param programId   the UUID of the grant program
     * @return {@code true} if a duplicate application exists; {@code false} otherwise
     */
    boolean existsByApplicantIdAndProgramId(UUID applicantId, UUID programId);

    /**
     * Retrieves all applications submitted by a specific applicant, supporting the
     * "My Applications" dashboard view.
     *
     * @param applicantId the UUID of the applicant whose applications are requested
     * @return a list of {@link Application} entities belonging to the applicant;
     *         empty if the applicant has no applications on record
     */
    List<Application> findAllByApplicantId(UUID applicantId);
}