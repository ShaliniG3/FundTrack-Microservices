package com.cts.fundtrack.common.exceptions;

import java.util.UUID;

/**
 * Thrown when an applicant attempts to submit a second application for a program
 * they have already applied to.
 *
 * <p>The system enforces a one-application-per-program constraint. This exception
 * is raised by the Application Service when a duplicate submission is detected.
 * Mapped to {@code HTTP 409 Conflict} by the {@link GlobalExceptionHandler}.</p>
 */
public class DuplicateApplicationException extends RuntimeException {

    /**
     * Constructs the exception with a message that identifies both the applicant
     * and the program involved in the duplicate submission attempt.
     *
     * @param applicantId the UUID of the applicant who already has an existing application
     * @param programId   the UUID of the program the applicant is trying to apply to again
     */
    public DuplicateApplicationException(UUID applicantId, UUID programId) {
        super("Applicant " + applicantId + " has already submitted an application for program " + programId);
    }
}