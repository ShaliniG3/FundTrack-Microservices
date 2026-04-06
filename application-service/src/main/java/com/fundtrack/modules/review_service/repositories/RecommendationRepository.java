package com.fundtrack.modules.review_service.repositories;


import com.fundtrack.modules.review_service.models.Recommendation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
    // Custom query to find the recommendation for an application

    void deleteByApplicationId(UUID applicationId);

    Optional<Recommendation> findByApplicationId(UUID applicationId);
}
