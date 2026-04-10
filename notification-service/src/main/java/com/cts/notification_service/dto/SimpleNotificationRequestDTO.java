package com.cts.notification_service.dto;

import lombok.Data;

import java.util.UUID;


/**
 * Data Transfer Object for sending a basic, non-templated notification.
 * Used primarily for one-off manual messages where category-based
 * templates are not required.
 */
@Data
public class SimpleNotificationRequestDTO {

    /** The unique identifier of the recipient user. */
    private UUID userId;

    /** The custom text message to be sent to the user. */
    private String message;

    // Getters and Setters with standard Javadoc...
}


