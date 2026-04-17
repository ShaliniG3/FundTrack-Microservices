package com.cts.fundtrack.application.repository;

import com.cts.fundtrack.application.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Decision} entities.
 *
 * <p>Provides persistence operations for final funding decisions recorded by
 * approvers in the FundTrack grant management system. Each application has at
 * most one active decision record; decisions can be updated or deleted (revoked)
 * through the {@link com.cts.fundtrack.application.service.DecisionService}.</p>
 */
@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {

    /**
     * Retrieves the final decision record for a specific grant application.
     *
     * <p>Used during decision processing, amendment, and revocation workflows,
     * as well as for audit trail and compliance reporting purposes.</p>
     *
     * @param applicationId the UUID of the application whose decision is requested
     * @return an {@link Optional} containing the {@link Decision} if one exists,
     *         or empty if no decision has been recorded for this application
     */
    Optional<Decision> findByApplicationId(UUID applicationId);

    /**
     * Retrieves all decisions made by a specific approver.
     *
     * <p>Used to populate the approver's historical decision dashboard and to
     * support performance metrics and workload tracking.</p>
     *
     * @param approverId the UUID of the approver whose decision history is requested
     * @return a list of {@link Decision} entities made by the approver; empty if
     *         none exist
     */
    List<Decision> findByApproverId(UUID approverId);
}