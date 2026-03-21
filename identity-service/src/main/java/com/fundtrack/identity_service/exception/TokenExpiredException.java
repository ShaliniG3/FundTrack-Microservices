package com.fundtrack.identity_service.exception;

/**
 * Exception thrown when a security token (JWT, Reset, or Refresh)
 * has passed its expiration time.
 */
public class TokenExpiredException extends RuntimeException {

    /**
     * Constructs the exception with a detailed message.
     * * @param message description of the expired token and required action.
     */
    public TokenExpiredException(String message) {
        super(message);
    }
}