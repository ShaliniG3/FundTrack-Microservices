package com.cts.fundtrack.identity.repository;

import com.cts.fundtrack.identity.model.User;         // Updated to identity model
import com.cts.fundtrack.identity.model.RefreshToken; // Updated to identity model
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for managing {@link RefreshToken} entities.
 * <p>
 * Extends {@link JpaRepository} to provide CRUD operations for refresh tokens,
 * including custom methods for fetching and deleting tokens associated with users.
 * <p>
 * Used by the authentication layer to support token refresh workflows and
 * cleanup operations during logout or session invalidation.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its token string value.
     *
     * @param token the raw refresh token string
     * @return an {@link Optional} containing the matching {@link RefreshToken} if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens associated with a specific user.
     * <p>
     * This is typically invoked during logout or when enforcing
     * a single‑session policy.
     *
     * @param user the user whose refresh tokens should be removed
     */
    void deleteByUser(User user);
}