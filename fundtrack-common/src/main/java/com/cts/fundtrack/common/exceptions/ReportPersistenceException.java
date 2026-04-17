package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when the system successfully processes a grant report file but subsequently
 * fails to persist the associated metadata record to the database.
 *
 * <p>This exception signals a partial-success scenario: the file was uploaded and stored
 * successfully, but the database record could not be saved — for example, due to a
 * transaction failure, constraint violation, or transient infrastructure error.
 * Mapped to {@code HTTP 500 Internal Server Error} by the {@link GlobalExceptionHandler}.</p>
 */
public class ReportPersistenceException extends RuntimeException {

    /**
     * Constructs the exception with a message describing the persistence failure.
     *
     * @param message human-readable description of the database save failure
     */
    public ReportPersistenceException(String message) {
        super(message);
    }
}