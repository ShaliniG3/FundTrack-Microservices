package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.Data;

/**
 * Data Transfer Object representing the response payload for a simple, non-templated
 * notification operation.
 *
 * <p>Returned by the Notification Service after processing a {@link SimpleNotificationRequestDTO}.
 * Echoes back the recipient and the message that was dispatched, allowing the caller to
 * confirm what was sent without needing to re-query the notification store.</p>
 *
 * <p>Note: the class name contains a typo ({@code Notifcation} instead of
 * {@code Notification}) preserved for backward binary compatibility.</p>
 */
@Data
public class SimpleNotifcationResponseDTO {

    /** The unique identifier of the recipient user to whom the notification was sent. */
    private UUID userId;

    /** The custom text message that was dispatched to the user. */
    private String message;
}