package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a compliance audit action is attempted on a grant report that has already
 * reached a terminal state ({@code APPROVED} or {@code REJECTED}).
 *
 * <p>Once a grant report has been given a final compliance verdict, its state is
 * immutable. This exception prevents duplicate audit operations and protects the integrity
 * of closed audit records. Mapped to {@code HTTP 409 Conflict}
 * by the {@link GlobalExceptionHandler}.</p>
 */
public class ReportLockedException extends RuntimeException {

    /**
     * Constructs the exception with a message explaining why the report cannot be modified.
     *
     * @param message human-readable description identifying the report and its terminal state
     */
    public ReportLockedException(String message) {
        super(message);
    }
}