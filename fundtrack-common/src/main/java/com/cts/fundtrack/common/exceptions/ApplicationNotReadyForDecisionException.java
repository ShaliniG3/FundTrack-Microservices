package com.cts.fundtrack.common.exceptions;

import java.util.UUID;

/**
 * Thrown when an Approver attempts to render a final decision on an application
 * that is not in the required {@code UNDER_REVIEW} state.
 *
 * <p>Only applications that have completed the reviewer phase and are in
 * {@code UNDER_REVIEW} status are eligible for a final APPROVED or REJECTED decision.
 * This exception guards against decisions being applied to applications that are
 * still in {@code DRAFT}, {@code SUBMITTED}, or already in a terminal state.</p>
 *
 * <p>Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class ApplicationNotReadyForDecisionException extends RuntimeException {

    /**
     * Constructs the exception with a message that includes the application ID
     * and its current disqualifying status.
     *
     * @param id            the UUID of the application that cannot be decided upon
     * @param currentStatus the application's actual status at the time of the attempt
     */
    public ApplicationNotReadyForDecisionException(UUID id, String currentStatus) {
        super("Application " + id + " is in status '" + currentStatus +
            "'. Only applications UNDER_REVIEW can be finalized.");
    }
}