package com.cts.fundtrack.common.exceptions;

import java.util.UUID;

/**
 * Thrown when a Reviewer attempts to evaluate an application that has not yet
 * reached {@code SUBMITTED} status.
 *
 * <p>Only applications that have passed initial validation and are in {@code SUBMITTED}
 * state are eligible to enter the review queue. This exception prevents Reviewers from
 * working on applications still in {@code DRAFT} or already beyond the review phase.</p>
 *
 * <p>Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class ApplicationNotSubmittedException extends RuntimeException {

    /**
     * Constructs the exception with a message that includes the application ID
     * and its current disqualifying status.
     *
     * @param id            the UUID of the application that cannot be reviewed
     * @param currentStatus the application's actual status at the time of the attempt
     */
    public ApplicationNotSubmittedException(UUID id, String currentStatus) {
        super("Application " + id + " is in status '" + currentStatus +
            "'. Only applications in 'SUBMITTED' status can be reviewed.");
    }
}