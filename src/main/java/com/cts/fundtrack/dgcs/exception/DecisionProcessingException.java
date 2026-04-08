package com.cts.fundtrack.dgcs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the system encounters a terminal error while processing
 * a final decision on a grant application.
 * <p>
 * This exception is typically triggered in the Service layer during:
 * <ul>
 * <li>Transitioning application status (e.g., Approve/Reject logic)</li>
 * <li>Conflict between automated validation results and manual overrides</li>
 * <li>Failure to finalize financial allocations during the approval phase</li>
 * </ul>
 * </p>
 * <p>
 * Returns {@link HttpStatus#BAD_REQUEST} (400) to indicate that the decision
 * request was malformed or could not be logically completed.
 * </p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DecisionProcessingException extends RuntimeException {

    /**
     * Constructs a new DecisionProcessingException with a detailed error message.
     *
     * @param message the detail message describing the specific reason the
     * decision process failed.
     */
    public DecisionProcessingException(String message) {
        super(message);
    }
}