package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.Data;


/**
 * Data Transfer Object for sending a basic, non-templated notification to a user.
 *
 * <p>Used primarily for one-off manual messages — for example, administrative
 * announcements or custom alerts — where the category-based template engine
 * in {@link com.cts.fundtrack.common.models.enums.NotificationTemplate} is not
 * required. The message content is used verbatim as the notification body.</p>
 */
@Data
public class SimpleNotificationRequestDTO {

    /** Unique identifier of the user who will receive the notification. */
    private UUID userId;

    /** The custom text message to be delivered to the user as-is. */
    private String message;
}

