package com.cts.fundtrack.common.exceptions;

/**
 * Specifically thrown when a GrantReport lookup fails.
 */
public class GrantReportNotFoundException extends ResourceNotFoundException {
    public GrantReportNotFoundException(String message) {
        super(message);
    }
}