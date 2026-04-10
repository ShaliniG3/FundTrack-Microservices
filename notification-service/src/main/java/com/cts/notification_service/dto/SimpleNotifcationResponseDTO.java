package com.cts.notification_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SimpleNotifcationResponseDTO {

    /** The unique identifier of the recipient user. */
    private UUID userId;

    /** The custom text message to be sent to the user. */
    private String message;

}
