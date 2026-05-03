package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a reviewer recommendation cannot be located in the persistence layer.
 *
 * <p>Raised by the Review or Application Service when a lookup for a recommendation
 * by its identifier returns no matching record. Typically mapped to
 * {@code HTTP 404 Not Found} by the {@link GlobalExceptionHandler}.</p>
 */
public class RecommendationNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message identifying the missing recommendation.
     *
     * @param message human-readable description indicating which recommendation was not found
     */
    public RecommendationNotFoundException(String message) {
        super(message);
    }
}
