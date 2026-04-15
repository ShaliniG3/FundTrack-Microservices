package com.cts.fundtrack.common.dto;

import java.util.UUID;

import com.cts.fundtrack.common.models.enums.NotificationCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Data Transfer Object for incoming notification requests.
 * This class captures the raw data sent by the client or internal services
 * and applies validation constraints before the data reaches the service layer.
 * It serves as the primary input for both manual and templated notifications.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequestDTO {

    /**
     * The unique identifier (UUID) of the user who will receive the notification.
     * This field is mandatory as it defines the "owner" of the notification record.
     */
    @NotNull(message = "User ID is required")
    private UUID userId;

    /**
     * The unique identifier of the specific application or grant related to this notification.
     * This field is optional. If provided, it is used by the template engine
     * to inject application-specific reference numbers into the message.
     */
    private UUID applicationId;

    /**
     * The actual text content of the notification to be displayed to the user.
     * If this field is provided, it will override the default template logic.
     * Cannot be null or contain only whitespace.
     */
    @NotBlank(message = "Message cannot be empty")
    private String message;

    /**
     * The classification of the notification (e.g., 'APPROVAL', 'DISBURSED', 'WELCOME').
     * This string must correspond to a valid {@code NotificationCategory} enum value
     * for the template engine to function correctly.
     */

    private NotificationCategory category;
}

