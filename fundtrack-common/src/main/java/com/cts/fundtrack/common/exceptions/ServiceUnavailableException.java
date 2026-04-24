package com.cts.fundtrack.common.exceptions;

/**
 * Thrown when a required downstream service is temporarily unreachable,
 * indicating that the operation should be retried later rather than treated
 * as a permanent failure.
 *
 * <p>Typically thrown by Feign client fallbacks (circuit breakers) when a
 * dependent microservice does not respond within the expected time or returns
 * repeated errors. Callers should catch this exception and either defer the
 * operation, surface a {@code 503 Service Unavailable} response to the client,
 * or apply a safe degradation strategy without mutating application state.</p>
 */
public class ServiceUnavailableException extends RuntimeException {

    /**
     * Constructs a new {@code ServiceUnavailableException} with the specified detail message.
     *
     * @param message a human-readable description of which service is unavailable
     *                and what operation was being attempted
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }
}