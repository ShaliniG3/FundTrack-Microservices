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
 * Utility class for generating, parsing, and validating JSON Web Tokens (JWT).
 * Updated to include userId claims for microservice auditing.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.token.expiration}")
    private Long tokenExpirationMs;

    private SecretKey key;

    @jakarta.annotation.PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.debug("JwtUtil initialized with secure signing key");
    }

    /**
     * UPDATED: Now accepts userId to ensure the Gateway can pass it to other services.
     */
    public String generate(String email, String role, UUID userId) {
        log.info("Generating JWT for user: {} | Role: {} | ID: {}", email, role, userId);

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(tokenExpirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId.toString()) // <--- THE MISSING LINK
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    /**
     * ADDED: Helper to extract the userId claim.
     */
    public String extractUserId(String token) {
        return parse(token).get("userId", String.class);
    }

    public boolean isExpired(String token) {
        return parse(token).getExpiration().before(new Date());
    }

    public boolean isValid(String token, User user) {
        // Changed to .getEmail() to match your likely User model field
        return extractEmail(token).equals(user.getEmail()) && !isExpired(token);
    }

    public Date getExpirationTime(String token) {
        return parse(token).getExpiration();
    }
}