package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when an audit action is attempted on a report that is
 * already in a final (terminal) state like APPROVED or REJECTED.
 */
public class ReportLockedException extends RuntimeException {
    public ReportLockedException(String message) {
        super(message);
    }
}