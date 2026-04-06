package com.fundtrack.modules.review_service.repositories;


import com.fundtrack.modules.review_service.models.Review;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    // Custom query to find all reviews for a specific application
    
    Optional<Review> findByApplicationId(UUID applicationId);

    void deleteByApplicationId(UUID applicationId);

    List<Review> findByReviewerId(UUID reviewerId);
}
