package com.cts.fundtrack.identity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity representing a refresh token issued to an authenticated user.
 *
 * <p>Refresh tokens are long-lived credentials stored in the {@code refresh_tokens}
 * table. They allow a client to obtain a new short-lived JWT access token without
 * requiring the user to re-enter their credentials. Each record is uniquely
 * identified by its {@code token} string (a random UUID value).</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>A single-session policy is enforced by deleting any existing refresh token
 *       for a user before issuing a new one (see
 *       {@link com.cts.fundtrack.identity.service.RefreshTokenServiceImpl#createRefreshToken}).</li>
 *   <li>The {@link User} association is lazily fetched; callers must be inside an
 *       active transaction (or use join-fetch) to access {@code user} without a
 *       {@code LazyInitializationException}.</li>
 *   <li>Expiry is checked at the service layer via
 *       {@link com.cts.fundtrack.identity.service.RefreshTokenService#verifyExpiration};
 *       expired tokens are deleted immediately upon detection.</li>
 * </ul>
 *
 * @see com.cts.fundtrack.identity.service.RefreshTokenService
 * @see com.cts.fundtrack.identity.repository.RefreshTokenRepository
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    /**
     * Auto-incremented surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The opaque refresh token string presented by the client.
     *
     * <p>Generated as a random UUID at issuance time. Must be unique across all
     * active tokens and is stored with a maximum length of 100 characters.</p>
     */
    @Column(nullable = false, unique = true, length = 100)
    private String token;

    /**
     * The UTC instant after which this refresh token is no longer valid.
     *
     * <p>Compared against {@link Instant#now()} by
     * {@link com.cts.fundtrack.identity.service.RefreshTokenService#verifyExpiration}
     * to enforce token expiry.</p>
     */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * The user to whom this refresh token was issued.
     *
     * <p>Many refresh tokens can exist for the same user (across devices/sessions),
     * though the current implementation enforces a single active token per user.
     * The association is lazily fetched and is required ({@code optional = false}).</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
