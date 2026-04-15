package com.cts.fundtrack.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an error occurs during the aggregation or calculation
 * of compliance metrics and reporting data.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ComplianceDataException extends RuntimeException {

    public ComplianceDataException(String message) {
        super(message);
    }

    public ComplianceDataException(String message, Throwable cause) {
        super(message, cause);
    }
}