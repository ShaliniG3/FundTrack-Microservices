package com.fundtrack.identity_service.service;

import com.fundtrack.identity_service.exception.InvalidTokenException;
import com.fundtrack.identity_service.exception.UserNotFoundException;
import com.fundtrack.identity_service.model.RefreshToken;
import com.fundtrack.identity_service.model.User;
import com.fundtrack.identity_service.repository.RefreshTokenRepository;
import com.fundtrack.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for the lifecycle of refresh tokens:
 * creation, validation (expiration check), lookup, and deletion.
 * <p>
 * This service supports token-based authentication flows by issuing
 * a long-lived refresh token that can be used to obtain new access tokens.
 * <p>
 * SLF4J logging is included for observability while avoiding sensitive data
 * (e.g., the actual token values are not logged).
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    /**
     * Refresh token validity duration in milliseconds.
     * Value is injected from application properties {@code jwt.refresh.expiration}.
     */
    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Creates (and persists) a new refresh token for the given user email.
     * <p>
     * Existing tokens for the user are deleted to enforce single-session semantics.
     *
     * @param email the email of the user for whom to create a refresh token
     * @return the newly created {@link RefreshToken}
     * @throws RuntimeException if the user cannot be found for the provided email
     */
    public RefreshToken createRefreshToken(String email) {

        log.info("Creating refresh token for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Cannot create refresh token: user not found for email={}", email);
                    return new UserNotFoundException("User not found");
                });

        // Optional: remove old refresh token (one session per user)
        refreshTokenRepository.deleteByUser(user);
        log.debug("Deleted existing refresh tokens for userId={}", user.getUserId());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for userId={}, expiresAt={}", user.getUserId(), saved.getExpiryDate());
        // Intentionally not logging the token value

        return saved;
    }

    /**
     * Verifies that the given refresh token has not expired.
     * <p>
     * If the token is expired, it is deleted and an exception is thrown.
     *
     * @param token the {@link RefreshToken} to validate
     * @return the same token if it is still valid
     * @throws RuntimeException if the token has expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Refresh token expired for userId={}, deleting token", token.getUser().getUserId());
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token expired. Please login again.");
        }

        log.debug("Refresh token is valid for userId={}, expiresAt={}",
                token.getUser().getUserId(), token.getExpiryDate());
        return token;
    }

    /**
     * Finds a refresh token by its raw token string.
     *
     * @param token the raw token string
     * @return an {@link Optional} containing the token if found
     */
    public Optional<RefreshToken> findByToken(String token) {
        log.debug("Finding refresh token by token string (value hidden)");
        // If findByToken returns empty, the .orElseThrow() triggers.
        RefreshToken foundToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.error("Security Alert: Provided refresh token does not exist in the database.");
                    return new InvalidTokenException("Invalid refresh token. Access denied.");
                });

        return Optional.of(foundToken);
    }

    /**
     * Deletes all refresh tokens associated with the specified user.
     * <p>
     * Typically used during logout or session invalidation.
     *
     * @param user the {@link User} whose refresh tokens should be deleted
     */
    public void deleteByUser(User user) {
        log.info("Deleting refresh tokens for userId={}", user.getUserId());
        refreshTokenRepository.deleteByUser(user);
    }
}



