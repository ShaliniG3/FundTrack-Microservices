package com.cts.fundtrack.disbursement.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.disbursement.models.ComplianceCheck;

/**
 * Spring Data JPA repository for {@link ComplianceCheck} entities.
 * <p>
 * Provides data access operations for compliance audit records. Each
 * {@link ComplianceCheck} represents an immutable, timestamped audit decision
 * made by a Compliance Officer on a submitted grant report. This repository
 * supports audit history retrieval and officer-level accountability reporting.
 * </p>
 */
@Repository
public interface ComplianceCheckRepository extends JpaRepository<ComplianceCheck, UUID> {

    /**
     * Retrieves all compliance audit records performed by a specific officer,
     * providing a full traceable history of their audit decisions.
     *
     * @param complianceOfficerId the UUID of the compliance officer whose audit
     *                            history is requested
     * @return a list of {@link ComplianceCheck} records associated with that officer;
     *         an empty list if the officer has no recorded audits
     */
    List<ComplianceCheck> findByComplianceOfficerId(UUID complianceOfficerId);
//
//
//    /**
//     * Retrieves all audits for the history view.
//     * This works because it only queries your local 'compliance_checks' table.
//     */
//    @Query("SELECT c FROM ComplianceCheck c ORDER BY c.date DESC")
//    List<ComplianceCheck> findAllAudits();
//
//    /**
//     * Find all checks for a specific application.
//     */
//    List<ComplianceCheck> findByApplicationId(UUID applicationId);
//
//    /**
//     * Check if an audit exists for a specific application.
//     */
//    boolean existsByApplicationId(UUID applicationId);

}