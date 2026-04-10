package com.cts.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;

/**
 * Exception thrown when a requested notification cannot be located in the system.
 * <p>
 * This exception is annotated with {@link ResponseStatus} to automatically
 * return an {@code HTTP 404 NOT FOUND} status code to the client when thrown
 * from a controller.
 * </p>
 *
 * @author Gemini
 * @version 1.0
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends RuntimeException {

    /**
     * Constructs a new NotificationNotFoundException with a formatted message
     * containing the missing notification's unique identifier.
     *
     * @param string The {@link UUID} of the notification that was not found.
     */
    public NotificationNotFoundException(String string ) {
        super(String.format("Notification with ID %s was not found.", string));
    }
}
