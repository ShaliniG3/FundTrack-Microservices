package com.fundtrack.modules.decision_service.repository;


import com.fundtrack.modules.decision_service.models.Decision;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRepository extends JpaRepository<Decision, UUID> {
    
    /**
     * Finds the final decision record for a specific application.
     * Useful for audit trails and compliance reporting[cite: 6, 20].
     */
    Optional<Decision> findByApplicationId(UUID applicationId);
    List<Decision> findByApproverId(UUID approverId);
}