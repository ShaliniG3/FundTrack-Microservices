package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a Reviewer submits a score that falls outside the permitted range of 0–100.
 *
 * <p>Raised by the Review Service during score validation before persisting the review.
 * Prevents outlier values that would skew ranking and decision analytics.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class InvalidReviewScoreException extends RuntimeException {

    /**
     * Constructs the exception with a message that includes the offending score value.
     *
     * @param score the invalid score submitted by the Reviewer
     */
    public InvalidReviewScoreException(Integer score) {
        super("The provided score '" + score + "' is invalid. Score must be between 0 and 100.");
    }
}