package com.cts.fundtrack.application.repository;

import com.cts.fundtrack.application.model.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {
    
    /**
     * Finds the final decision record for a specific application.
     * Useful for audit trails and compliance reporting.
     */
    Optional<Decision> findByApplicationId(UUID applicationId);

    /**
     * Finds all decisions made by a specific Approver.
     * Useful for performance metrics and workload tracking.
     */
    List<Decision> findByApproverId(UUID approverId);
}