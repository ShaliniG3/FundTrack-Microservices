package com.cts.fundtrack.common.exceptions;

/**
 * Base exception thrown when any requested resource cannot be located in the system.
 *
 * <p>Serves as the common supertype for all domain-specific not-found exceptions
 * (e.g., {@link GrantReportNotFoundException}). May also be thrown directly when
 * no more-specific subtype is applicable.
 * Mapped to {@code HTTP 404 Not Found} by the {@link GlobalExceptionHandler}.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message identifying the missing resource.
     *
     * @param message human-readable description indicating which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}