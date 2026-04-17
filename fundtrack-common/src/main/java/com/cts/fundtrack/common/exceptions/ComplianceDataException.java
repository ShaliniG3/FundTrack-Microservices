package com.cts.fundtrack.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an error occurs during the aggregation or calculation of compliance
 * metrics and reporting data in the Analytics or Disbursement modules.
 *
 * <p>Common triggers include database read failures when building compliance dashboards,
 * arithmetic errors during KPI calculation, or incomplete data sets that cannot produce
 * a meaningful result. Mapped to {@code HTTP 500 Internal Server Error} by the
 * {@link GlobalExceptionHandler} and the {@code @ResponseStatus} annotation.</p>
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ComplianceDataException extends RuntimeException {

    /**
     * Constructs the exception with a descriptive message.
     *
     * @param message human-readable description of the compliance data error
     */
    public ComplianceDataException(String message) {
        super(message);
    }

    /**
     * Constructs the exception with a descriptive message and the underlying cause.
     *
     * @param message human-readable description of the compliance data error
     * @param cause   the root exception that triggered this error
     */
    public ComplianceDataException(String message, Throwable cause) {
        super(message, cause);
    }
}