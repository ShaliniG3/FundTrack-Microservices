package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an authenticated user attempts to perform an operation they are
 * not permitted to carry out on the requested resource.
 *
 * <p>Distinct from an authentication failure (401 Unauthorized), this exception
 * represents an authorization failure — the user is known but lacks the required
 * role or ownership rights for the action. Mapped to {@code HTTP 403 Forbidden}
 * by the {@link GlobalExceptionHandler}.</p>
 *
 * <p>Common triggers include an Applicant trying to access another user's application,
 * or a Reviewer attempting to execute Approver-only decisions.</p>
 */
public class UnauthorizedAccessException extends RuntimeException {

    /**
     * Constructs the exception with a message explaining the authorization failure.
     *
     * @param message human-readable description of why access was denied
     */
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
