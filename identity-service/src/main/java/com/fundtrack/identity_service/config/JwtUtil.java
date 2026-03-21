package com.fundtrack.identity_service.config;

import com.fundtrack.identity_service.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Utility class for generating, parsing, and validating JSON Web Tokens (JWT).
 * <p>
 * This class:
 * <ul>
 * <li>Creates JWT tokens with subject and role claims</li>
 * <li>Parses incoming tokens to extract claims</li>
 * <li>Validates expiration and compares token data with user details</li>
 * </ul>
 * It uses {@link Jwts} for token operations and an HMAC SHA key for signing.
 */

@Slf4j
@Component
@Tag(name = "Authentication Utility", description = "Internal utility for JWT management and security token operations")
public class JwtUtil {
    /**
     * Secret key value loaded from application properties.
     * Must be a sufficiently long string to satisfy HMAC SHA requirements.
     */
    @Value("${jwt.secret}")
    @Schema(hidden = true) // Hide sensitive internal fields from Swagger UI
    private String jwtSecret;

    /**
     * The computed signing key derived from the SECRET after initialization.
     */
    @Schema(hidden = true)
    private SecretKey key;

    /**
     * Initializes the signing key once the bean is constructed.
     * <p>
     * This method converts the configured secret into a secure {@link SecretKey}
     * suitable for HMAC SHA signing.
     */
    @jakarta.annotation.PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.debug("JwtUtil initialized and signing key generated successfully");
    }

    /**
     * Generates a JWT token for the given user email and role.
     */

    @Value("${jwt.expiration}")
    @Schema(description = "Token lifespan in milliseconds", example = "300000")
    private Long tokenExpirationMs; //  15 minutes

    public String generate(String email, String role) {
        log.info("Generating JWT for user with role={}", role);

        // Capture the current point in time
        Instant now = Instant.now();
        // Calculate expiration using Instant's fluent API
        Instant expiry = now.plusMillis(tokenExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Parses the JWT token and extracts the claims.
     *
     * @param token the raw JWT string
     * @return the extracted {@link Claims} object
     * @throws JwtException if parsing fails or token is invalid
     */
    public Claims parse(String token) {
        log.debug("Parsing JWT token (content not logged for security)");

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        log.debug("Successfully parsed JWT claims");
        return claims;
    }

    /**
     * Extracts the subject (email) from a given token.
     *
     * @param token the JWT token
     * @return email stored as token subject
     */
    public String extractEmail(String token) {
        log.debug("Extracting email from JWT");
        return parse(token).getSubject();
    }

    /**
     * Extracts the role stored inside the JWT.
     *
     * @param token the JWT token
     * @return the role claim value
     */
    public String extractRole(String token) {
        log.debug("Extracting role from JWT");
        return parse(token).get("role", String.class);
    }

    /**
     * Checks whether the token is expired.
     *
     * @param token the JWT token
     * @return true if expired, false otherwise
     */
    public boolean isExpired(String token) {
        log.debug("Checking if JWT is expired");
        return parse(token).getExpiration().before(new Date());
    }

    /**
     * Validates the token by checking:
     * <ul>
     * <li>Whether the subject matches the user's username</li>
     * <li>Whether the token is not expired</li>
     * </ul>
     *
     * @param token the JWT token
     * @param user  the user object to validate against
     * @return true if token is valid and matches the user
     */
    public boolean isValid(String token, User user) {
        log.debug("Validating JWT for user (email not logged)");
        return extractEmail(token).equals(user.getUsername()) && !isExpired(token);
    }

    /**
     * Retrieves the expiration time of the token.
     *
     * @param token the JWT token
     * @return expiration timestamp as {@link Date}
     */
    public Date getExpirationTime(String token) {
        log.debug("Fetching JWT expiration time");
        return parse(token).getExpiration();
    }
}