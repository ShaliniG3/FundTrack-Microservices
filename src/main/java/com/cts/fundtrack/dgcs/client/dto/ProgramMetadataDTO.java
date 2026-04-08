package com.cts.fundtrack.dgcs.client.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramMetadataDTO {
    private UUID programId;
    private String name;
    private String status; // To check if it is "CLOSED"
    private Double budget; // To calculate the individual share
}