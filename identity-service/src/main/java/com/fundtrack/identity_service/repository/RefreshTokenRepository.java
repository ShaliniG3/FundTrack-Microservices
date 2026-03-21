package com.fundtrack.identity_service.repository;

import com.fundtrack.identity_service.model.RefreshToken;
import com.fundtrack.identity_service.model.User;
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


