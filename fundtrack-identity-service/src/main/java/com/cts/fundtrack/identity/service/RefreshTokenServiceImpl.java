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
 * Default implementation of {@link RefreshTokenService} that manages the full
 * lifecycle of refresh tokens for the FundTrack Identity Service.
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Issuing a new refresh token for a user while enforcing a single-session
 *       policy (only one active token per user at a time).</li>
 *   <li>Verifying token expiry and immediately deleting expired tokens to prevent
 *       replay attacks.</li>
 *   <li>Looking up tokens by their raw string value for the token-refresh flow.</li>
 *   <li>Deleting all tokens for a user during logout.</li>
 * </ul>
 *
 * <p>Audit entries for token lifecycle events are captured automatically via the
 * {@link com.cts.fundtrack.identity.aspect.AuditAspect} AOP advice, which fires
 * on any method annotated with {@code @Auditable}.</p>
 *
 * <p>Token expiry duration is controlled by the {@code jwt.refresh.expiration}
 * application property (in milliseconds).</p>
 *
 * @see RefreshTokenService
 * @see RefreshToken
 * @see RefreshTokenRepository
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
     * Creates a new refresh token for the user identified by the given email address.
     *
     * <p>The operation is transactional to guarantee that deleting the old token
     * and persisting the new one occur atomically. This enforces the
     * single-session policy — at most one active refresh token per user.</p>
     *
     * <p>Annotated with {@code @Auditable} so a {@code CREATE} audit entry is
     * recorded on success.</p>
     *
     * @param email the email address of the user receiving the token
     * @return the newly created {@link RefreshToken} with a random UUID token string
     *         and an expiry instant offset by {@code jwt.refresh.expiration} milliseconds
     * @throws UserNotFoundException if no user is found for the supplied email
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.USER)
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
     * Verifies that the supplied refresh token has not yet expired.
     *
     * <p>If the token's {@code expiryDate} is in the past, it is deleted from the
     * data store immediately and an {@link InvalidTokenException} is thrown,
     * preventing further use. Callers must handle this exception and require
     * the user to log in again.</p>
     *
     * <p>Annotated with {@code @Auditable} so an {@code UPDATE} audit entry is
     * recorded on success.</p>
     *
     * @param token the {@link RefreshToken} to verify
     * @return the same {@link RefreshToken} instance if it has not expired
     * @throws InvalidTokenException if the token has passed its expiry date
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
     * Looks up a refresh token by its raw token string value.
     *
     * <p>If no matching token is found, an {@link InvalidTokenException} is thrown,
     * which prevents the {@code @Auditable} aspect from recording a success entry —
     * keeping the audit trail accurate for failed access attempts.</p>
     *
     * @param token the opaque refresh token string presented by the client
     * @return an {@link Optional} wrapping the matching {@link RefreshToken}
     * @throws InvalidTokenException if no token matching the provided string exists
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return Optional.of(refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token. Access denied.")));
    }

    /**
     * Deletes all refresh tokens associated with the given user and returns the
     * user's UUID so the AOP audit aspect can record the affected entity ID.
     *
     * <p>Annotated with {@code @Auditable} so a {@code DELETE} audit entry is
     * recorded on success. This method is called by
     * {@link com.cts.fundtrack.identity.service.AuthServiceImpl#logout} during
     * the logout flow.</p>
     *
     * @param user the {@link User} whose refresh tokens should be invalidated
     * @return the {@link UUID} of the user, required by the AOP audit aspect
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.USER)
    public UUID deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
        return user.getUserId(); // Required for AOP Aspect
    }
}