package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a grant application review record cannot be located in the persistence layer.
 *
 * <p>Raised by the Review Service when a lookup for a review by its identifier or
 * associated application yields no matching record. Mapped to
 * {@code HTTP 404 Not Found} by the {@link GlobalExceptionHandler}.</p>
 */
public class ReviewNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message identifying the missing review.
     *
     * @param message human-readable description indicating which review was not found
     */
    public ReviewNotFoundException(String message) {
        super(message);
    }
}
