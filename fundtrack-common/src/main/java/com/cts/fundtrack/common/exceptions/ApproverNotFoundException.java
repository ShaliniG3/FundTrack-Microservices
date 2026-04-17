package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an Approver user cannot be located in the system by the provided identifier.
 *
 * <p>Raised by the Decision Service when the specified approver ID does not correspond
 * to any user with the {@code APPROVER} role. Mapped to {@code HTTP 404 Not Found}
 * by the {@link GlobalExceptionHandler}.</p>
 */
public class ApproverNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message identifying the missing approver.
     *
     * @param message human-readable description indicating which approver was not found
     */
    public ApproverNotFoundException(String message) {
        super(message);
    }
}
