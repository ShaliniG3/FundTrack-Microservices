package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a generic entity lookup by identifier returns no result.
 *
 * <p>A general-purpose not-found exception used when no more specific typed exception
 * (e.g., {@link ApplicationNotFoundException}, {@link ProgramNotFoundException}) is
 * applicable. Mapped to {@code HTTP 404 Not Found} by the {@link GlobalExceptionHandler}.</p>
 */
public class EntityIdNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message identifying the missing entity.
     *
     * @param message human-readable description indicating which entity ID was not found
     */
    public EntityIdNotFoundException(String message) {
        super(message);
    }
}
