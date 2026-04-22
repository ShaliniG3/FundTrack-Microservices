package com.cts.fundtrack.identity.security;

import com.cts.fundtrack.identity.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Utility component responsible for generating, parsing, and validating
 * JSON Web Tokens (JWT) used throughout the Identity Service.
 *
 * <p>Tokens are signed with an HMAC-SHA key derived from the configured secret.
 * Each token carries three claims beyond the standard ones:</p>
 * <ul>
 *   <li>{@code sub} — the user's email address (standard JWT subject).</li>
 *   <li>{@code role} — the user's role string (e.g., {@code "ADMIN"}).</li>
 *   <li>{@code userId} — the user's UUID as a string, enabling downstream
 *       microservices to identify the actor without an additional database
 *       lookup.</li>
 * </ul>
 *
 * <p>Configuration properties required in {@code application.yml} / environment:</p>
 * <ul>
 *   <li>{@code app.jwt.secret} — minimum 256-bit (32-character) HMAC secret.</li>
 *   <li>{@code jwt.token.expiration} — access token TTL in milliseconds.</li>
 * </ul>
 *
 * <p>The signing key is initialised once at startup via {@link #init()} to avoid
 * re-deriving it on every token operation.</p>
 *
 * @see JwtFilter
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.token.expiration}")
    private Long tokenExpirationMs;

    private SecretKey key;

    /**
     * Derives the HMAC-SHA signing key from the configured secret string.
     *
     * <p>Called automatically by Spring after all properties have been injected
     * ({@code @PostConstruct}). The key is stored as a field so it is computed
     * only once per application lifecycle.</p>
     */
    @jakarta.annotation.PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.debug("JwtUtil initialized with secure signing key");
    }

    /**
     * Generates a signed JWT access token for the specified user.
     *
     * <p>The token includes:</p>
     * <ul>
     *   <li>{@code sub} — {@code email}</li>
     *   <li>{@code role} — {@code role}</li>
     *   <li>{@code userId} — string representation of {@code userId}</li>
     *   <li>{@code iat} — current UTC instant</li>
     *   <li>{@code exp} — current instant plus {@code jwt.token.expiration} ms</li>
     * </ul>
     *
     * @param email  the user's email address; used as the JWT subject
     * @param role   the user's role name (without the {@code ROLE_} prefix)
     * @param userId the user's UUID, embedded in the token for downstream services
     * @return a compact, signed JWT string
     */
    public String generate(String email, String role, UUID userId) {
        log.info("Generating JWT for user: {} | Role: {} | ID: {}", email, role, userId);

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(tokenExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Parses and validates a JWT string, returning its claims body.
     *
     * @param token the compact JWT string to parse
     * @return the {@link Claims} payload extracted from the token
     * @throws ExpiredJwtException   if the token's expiry time has passed
     * @throws JwtException          if the token signature is invalid or the token
     *                               is malformed
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the email address (JWT subject) from a token.
     *
     * @param token the compact JWT string
     * @return the email address embedded in the {@code sub} claim
     * @throws JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    /**
     * Extracts the role claim from a token.
     *
     * @param token the compact JWT string
     * @return the role string embedded in the {@code role} claim
     * @throws JwtException if the token is invalid or expired
     */
    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    /**
     * Extracts the {@code userId} claim from a token.
     *
     * <p>This claim is consumed by the API Gateway and other microservices to
     * identify the acting user without an extra database round-trip.</p>
     *
     * @param token the compact JWT string
     * @return the user's UUID as a string, as embedded in the {@code userId} claim
     * @throws JwtException if the token is invalid or expired
     */
    public String extractUserId(String token) {
        return parse(token).get("userId", String.class);
    }

    /**
     * Checks whether a token has passed its expiry instant.
     *
     * @param token the compact JWT string
     * @return {@code true} if the token's {@code exp} claim is before the current time
     * @throws JwtException if the token cannot be parsed
     */
    public boolean isExpired(String token) {
        return parse(token).getExpiration().before(new Date());
    }

    /**
     * Validates a token against a specific {@link User} instance.
     *
     * <p>Returns {@code true} only when both conditions hold:</p>
     * <ol>
     *   <li>The token's subject (email) matches {@code user.getEmail()}.</li>
     *   <li>The token has not expired.</li>
     * </ol>
     *
     * @param token the compact JWT string to validate
     * @param user  the {@link User} whose identity the token should represent
     * @return {@code true} if the token is valid for the given user
     * @throws JwtException if the token cannot be parsed
     */
    public boolean isValid(String token, User user) {
        return extractEmail(token).equals(user.getEmail()) && !isExpired(token);
    }

    /**
     * Returns the expiration {@link Date} embedded in a token.
     *
     * @param token the compact JWT string
     * @return the {@link Date} at which the token expires
     * @throws JwtException if the token cannot be parsed
     */
    public Date getExpirationTime(String token) {
        return parse(token).getExpiration();
    }
}
