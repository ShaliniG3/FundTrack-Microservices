package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a report submission is attempted outside the permitted
 * 90-day temporal window.
 */
public class ReportingWindowException extends RuntimeException {
    public ReportingWindowException(String message) {
        super(message);
    }
}