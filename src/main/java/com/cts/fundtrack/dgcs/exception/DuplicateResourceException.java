package com.cts.fundtrack.dgcs.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when attempting to create or register a resource
 * that already exists in the system.
 * <p>
 * This is commonly used for scenarios such as:
 * <ul>
 *     <li>Registering a user with an email that already exists</li>
 *     <li>Creating an entity that must remain unique</li>
 * </ul>
 *
 * The exception logs a warning when instantiated to aid in debugging
 * and monitoring duplicate‑creation attempts.
 */
@Slf4j
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new {@code DuplicateResourceException} with the provided message.
     *
     * @param message a descriptive message explaining the duplicate resource condition
     */
    public DuplicateResourceException(String message) {
        super(message);
        log.warn("DuplicateResourceException created: {}", message);
    }
}


