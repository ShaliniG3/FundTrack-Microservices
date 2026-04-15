package com.cts.fundtrack.application.repository;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cts.fundtrack.application.model.Recommendation;



public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
    // Custom query to find the recommendation for an application

    void deleteByApplicationId(UUID applicationId);

    Optional<Recommendation> findByApplicationId(UUID applicationId);
}