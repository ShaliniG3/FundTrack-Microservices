package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.identity.model.RefreshToken;
import com.cts.fundtrack.identity.model.User;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface defining the contract for managing refresh token lifecycle
 * operations within the FundTrack Identity Service.
 *
 * <p>Refresh tokens are long-lived credentials that allow authenticated clients
 * to obtain new short-lived JWT access tokens without re-submitting credentials.
 * This interface abstracts the creation, validation, lookup, and deletion of
 * those tokens.</p>
 *
 * <p>The primary implementation is {@link RefreshTokenServiceImpl}, which enforces
 * a single-active-token-per-user policy and delegates expiry verification to this
 * contract.</p>
 *
 * @see RefreshToken
 * @see RefreshTokenServiceImpl
 */
public interface RefreshTokenService {

    /**
     * Creates and persists a new refresh token for the user identified by the
     * given email address.
     *
     * <p>Implementations must enforce a single-session policy by deleting any
     * existing refresh token for this user before issuing the new one.</p>
     *
     * @param email the email address of the user for whom the refresh token is created
     * @return the newly created and persisted {@link RefreshToken}
     * @throws com.cts.fundtrack.common.exceptions.UserNotFoundException if no user
     *         exists with the provided email
     */
    RefreshToken createRefreshToken(String email);

    /**
     * Verifies that the given refresh token has not passed its expiry date.
     *
     * <p>If the token is expired, implementations must delete it from the
     * data store and throw an exception to prevent replay use.</p>
     *
     * @param token the {@link RefreshToken} to validate
     * @return the same {@link RefreshToken} if it is still valid
     * @throws com.cts.fundtrack.common.exceptions.InvalidTokenException if the
     *         token has expired
     */
    RefreshToken verifyExpiration(RefreshToken token);

    /**
     * Looks up a refresh token by its raw token string value.
     *
     * @param token the opaque refresh token string presented by the client
     * @return an {@link Optional} containing the matching {@link RefreshToken},
     *         or throws if not found
     * @throws com.cts.fundtrack.common.exceptions.InvalidTokenException if no
     *         token with the given string exists in the data store
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens associated with the specified user and returns
     * the user's UUID for audit logging purposes.
     *
     * <p>This operation is typically called during logout to invalidate any
     * outstanding refresh tokens for the user.</p>
     *
     * @param user the {@link User} whose refresh tokens should be removed
     * @return the {@link UUID} of the user, used by the AOP audit aspect to
     *         capture the entity ID of the deleted tokens
     */
    UUID deleteByUser(User user);
}