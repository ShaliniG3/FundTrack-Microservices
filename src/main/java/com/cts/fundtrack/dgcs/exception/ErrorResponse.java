package com.cts.fundtrack.dgcs.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Standardized Data Transfer Object (DTO) for all API error responses.
 * <p>
 * feedback to the end-user.
 * </p>
 */
@Getter
@Builder
public class ErrorResponse {

    /**
     * The exact moment the error occurred.
     * Expressed in ISO-8601 format via {@link Instant}.
     */
    private Instant timestamp;

    /**
     * The numeric HTTP status code (e.g., 400, 404, 500).
     */
    private int status;

    /**
     * The official HTTP reason phrase corresponding to the status code (e.g., "Bad Request").
     */
    private String error;

    /**
     * A human-readable message describing the specific failure.
     * In production, this is sanitized for general errors but detailed for validation failures.
     */
    private String message;

    /**
     * The relative URI path where the exception was triggered (e.g., "/api/programs").
     */
    private String path;
}


