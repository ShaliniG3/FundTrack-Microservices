package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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