package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an error occurs while processing or persisting an approval decision.
 *
 * <p>Raised by the Decision Service when business rules are violated during decision
 * execution — for example, when the decision type is invalid, the application is already
 * in a terminal state, or a required downstream update fails.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class DecisionProcessingException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message explaining the processing failure.
     *
     * @param message human-readable description of why the decision could not be processed
     */
    public DecisionProcessingException(String message) {
        super(message);
    }
}
