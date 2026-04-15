package com.cts.fundtrack.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.application.model.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    /**
     * Retrieves the specific review associated with a grant application.
     */
    Optional<Review> findByApplicationId(UUID applicationId);

    /**
     * Deletes the review record. 
     * Note: This requires @Transactional at the service or repository level.
     */
    @Transactional
    void deleteByApplicationId(UUID applicationId);

    /**
     * Finds all reviews completed by a specific user in the Reviewer role.
     */
    List<Review> findByReviewerId(UUID reviewerId);
}