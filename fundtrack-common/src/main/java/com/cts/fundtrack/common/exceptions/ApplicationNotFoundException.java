package com.cts.fundtrack.common.exceptions;

import java.util.UUID;

/**
 * Thrown when a grant application lookup by UUID yields no result in the persistence layer.
 *
 * <p>Typically raised by the Application Service repository or service layer when a
 * client references an application ID that does not exist or has been deleted.
 * Mapped to {@code HTTP 404 Not Found} by the {@link GlobalExceptionHandler}.</p>
 */
public class ApplicationNotFoundException extends RuntimeException {

    /**
     * Constructs the exception with a message that includes the missing application ID.
     *
     * @param id the UUID of the application that could not be found
     */
    public ApplicationNotFoundException(UUID id) {
        super("Application with ID " + id + " was not found.");
    }
}