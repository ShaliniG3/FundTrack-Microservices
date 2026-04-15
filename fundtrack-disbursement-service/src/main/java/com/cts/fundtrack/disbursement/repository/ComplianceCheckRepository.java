package com.cts.fundtrack.disbursement.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.disbursement.models.ComplianceCheck;

@Repository
public interface ComplianceCheckRepository extends JpaRepository<ComplianceCheck, UUID> {

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