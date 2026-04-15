package com.cts.fundtrack.identity.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.exceptions.InvalidTokenException;
import com.cts.fundtrack.common.exceptions.UserNotFoundException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.identity.model.RefreshToken;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.repository.RefreshTokenRepository;
import com.cts.fundtrack.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for the lifecycle of refresh tokens.
 * Auditing is handled centrally via AOP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new refresh token. 
     * Uses @Transactional to ensure the deletion of old tokens and 
     * the creation of the new one happen atomically.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.USER) // Or EntityType.TOKEN if you have it
    public RefreshToken createRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        // Enforce single-session: remove existing tokens before issuing a new one
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verifies expiration. 
     * Audited to track token validation events.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.USER) 
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token expired. Please login again.");
        }
        return token;
    }

    /**
     * Look up a token. Exceptions here prevent the @Auditable aspect 
     * from firing a "SUCCESS" log, keeping your audit trail accurate.
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return Optional.of(refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token. Access denied.")));
    }

    /**
     * Deletes tokens for a user.
     * Changed return type to UUID so the Aspect can capture the entityId.
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.USER)
    public UUID deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        return user.getUserId(); // Required for AOP Aspect
    }
}