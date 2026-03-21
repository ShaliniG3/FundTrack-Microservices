package com.fundtrack.identity_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested security token (JWT or Refresh Token)
 * cannot be located in the persistence layer or the incoming request.
 * <p>
 * This typically occurs during token refresh attempts where the
 * provided refresh token ID does not exist in the database.
 * </p>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TokenNotFound extends RuntimeException {

    /**
     * Constructs a new TokenNotFound exception with a specific error message.
     *
     * @param message the detail message explaining which token was not found.
     */
    public TokenNotFound(String message) {
        super(message);
    }
}