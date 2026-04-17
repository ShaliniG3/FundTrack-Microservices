package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an operation violates the business rules governing the Program lifecycle.
 *
 * <p>Funding programs follow a strict state machine: {@code DRAFT → ACTIVE → CLOSED → ARCHIVED}.
 * This exception is raised when an invalid transition is attempted — for example, activating
 * a program before it meets all prerequisites, or submitting an application to a program
 * that is already {@code CLOSED} or {@code ARCHIVED}.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class ProgramLifecycleException extends RuntimeException {

    /**
     * Constructs the exception with a message describing the lifecycle rule violation.
     *
     * @param message human-readable description of the invalid lifecycle operation attempted
     */
    public ProgramLifecycleException(String message) {
        super(message);
    }
}

