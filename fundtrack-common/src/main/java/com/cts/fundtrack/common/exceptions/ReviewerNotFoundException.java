package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a Reviewer user cannot be located in the system by the provided identifier.
 *
 * <p>Raised by the Review or Application Service when the specified reviewer ID does not
 * correspond to any user with the {@code REVIEWER} role. Mapped to
 * {@code HTTP 404 Not Found} by the {@link GlobalExceptionHandler}.</p>
 */
public class ReviewerNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message identifying the missing reviewer.
     *
     * @param message human-readable description indicating which reviewer was not found
     */
    public ReviewerNotFoundException(String message) {
        super(message);
    }
}
