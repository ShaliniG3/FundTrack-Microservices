package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an operation is attempted on a funding program that is in an
 * incompatible lifecycle state.
 *
 * <p>For example, trying to submit an application to a {@code CLOSED} or
 * {@code ARCHIVED} program, or attempting to activate a program that is still
 * in {@code DRAFT} state without meeting the activation prerequisites.
 * Mapped to {@code HTTP 400 Bad Request} by the {@link GlobalExceptionHandler}.</p>
 */
public class InvalidProgramStateException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message explaining the state conflict.
     *
     * @param message human-readable description of the invalid program state condition
     */
    public InvalidProgramStateException(String message) {
        super(message);
    }
}