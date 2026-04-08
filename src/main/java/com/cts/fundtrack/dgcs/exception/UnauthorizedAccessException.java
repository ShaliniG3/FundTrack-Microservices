package com.cts.fundtrack.dgcs.exception;

/**
 * Custom exception thrown when an authenticated user attempts to access a resource 
 * or perform an operation for which they do not have sufficient permissions.
 * <p>
 * This exception is typically mapped to an {@code HTTP 403 Forbidden} response 
 * in the {@link GlobalExceptionHandler}. It is distinct from 401 Unauthorized, 
 * as it implies the user is known but denied access to a specific business action.
 * </p>
 */
public class UnauthorizedAccessException extends RuntimeException {

    /**
     * Constructs a new UnauthorizedAccessException with a detailed message.
     * Use this to specify which role or permission was missing for the requested operation.
     *
     * @param message A descriptive message explaining the security violation.
     */
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}


