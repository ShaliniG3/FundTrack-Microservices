package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a grant progress report cannot be located in the persistence layer.
 *
 * <p>Extends {@link ResourceNotFoundException} to provide a domain-specific subtype
 * for grant report lookup failures. Raised by the Disbursement Service when a report
 * search by ID or application reference yields no matching record. Mapped to
 * {@code HTTP 404 Not Found} by the {@link GlobalExceptionHandler}.</p>
 */
public class GrantReportNotFoundException extends ResourceNotFoundException {

    /**
     * Constructs the exception with a message identifying the missing grant report.
     *
     * @param message human-readable description indicating which report was not found
     */
    public GrantReportNotFoundException(String message) {
        super(message);
    }
}