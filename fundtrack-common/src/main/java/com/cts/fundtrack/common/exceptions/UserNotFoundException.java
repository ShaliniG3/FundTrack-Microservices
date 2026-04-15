package com.cts.fundtrack.common.exceptions;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when a requested resource cannot be found in the system.
 * <p>
 * Common use cases include:
 * <ul>
 *     <li>Looking up a user by email or ID when no match exists</li>
 *     <li>Requesting an entity that has been deleted or never created</li>
 *     <li>Referencing an invalid identifier during operations</li>
 * </ul>
 *
 * A warning log entry is generated whenever this exception is instantiated
 * to assist with debugging and monitoring missing-resource cases.
 */
@Slf4j
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code NotFoundException} with the specified message.
     *
     * @param message a descriptive message explaining which resource was not found
     */
    public UserNotFoundException(String message) {
        super(message);
        log.warn("NotFoundException created: {}", message);
    }
}

