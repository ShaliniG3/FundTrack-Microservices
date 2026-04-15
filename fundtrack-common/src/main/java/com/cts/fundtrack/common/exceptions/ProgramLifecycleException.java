package com.cts.fundtrack.common.exceptions;

/**
 * Exception thrown when a requested operation violates the business rules 
 * of the Program lifecycle.
 * Thrown when a business rule is broken (e.g., submitting to a closed program).
 */
public class ProgramLifecycleException extends RuntimeException {
    public ProgramLifecycleException(String message) {
        super(message);
    }
}

