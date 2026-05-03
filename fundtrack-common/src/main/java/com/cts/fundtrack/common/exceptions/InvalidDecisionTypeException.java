package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a decision value other than {@code "APPROVED"} or {@code "REJECTED"}
 * is submitted to the Decision Service.
 *
 * <p>Only these two string values are accepted as final approval verdicts. Any other
 * value (including case variations not handled by the caller) triggers this exception.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class InvalidDecisionTypeException extends RuntimeException {

    /**
     * Constructs the exception with a message that includes the invalid decision value
     * and indicates the accepted alternatives.
     *
     * @param decision the invalid decision string that was provided by the caller
     */
    public InvalidDecisionTypeException(String decision) {
        super("'" + decision + "' is not a valid final decision. Use 'APPROVED' or 'REJECTED'.");
    }
}