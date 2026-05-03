package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an attempt is made to modify an application that is in a terminal
 * (immutable) lifecycle state.
 *
 * <p>Once an application reaches a final state such as {@code APPROVED} or
 * {@code REJECTED}, its data may no longer be changed. This exception is raised
 * by the Application Service to enforce that immutability constraint.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class InvalidApplicationStateException extends RuntimeException {

    /**
     * Constructs the exception with a message that includes the application's
     * current terminal status.
     *
     * @param status the application's current status that prevents modification
     */
    public InvalidApplicationStateException(String status) {
        super("Cannot modify application. Current status '" + status + "' is final.");
    }
}