package com.fundtrack.identity_service.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when an authentication token (e.g., JWT or session token)
 * is invalid, expired, malformed, or otherwise fails verification.
 * <p>
 * This exception is typically raised during authentication or authorization
 * flows when the system detects that the provided token cannot be trusted.
 * <p>
 * Logging is performed at WARN level for visibility without revealing
 * sensitive token data.
 */
@Slf4j
public class InvalidTokenException extends RuntimeException {

    /**
     * Constructs a new {@code InvalidTokenException} with the specified detail message.
     *
     * @param message a human-readable description of why the token is considered invalid
     */
    public InvalidTokenException(String message) {
        super(message);
        log.warn("InvalidTokenException created: {}", message);
    }

}


