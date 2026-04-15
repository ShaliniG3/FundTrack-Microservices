package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class SimpleNotifcationResponseDTO {

    /** The unique identifier of the recipient user. */
    private UUID userId;

    /** The custom text message to be sent to the user. */
    private String message;

}