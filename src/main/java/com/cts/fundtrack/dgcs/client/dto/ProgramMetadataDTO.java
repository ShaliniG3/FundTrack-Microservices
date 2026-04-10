package com.cts.fundtrack.dgcs.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

/**
 * Data Transfer Object for Program-level financial and lifecycle metadata.
 * <p>
 * Consumed by the Disbursement Service to retrieve the total allocatable budget
 * and verify that the program has reached the correct lifecycle stage (e.g., CLOSED)
 * before initiating the budget split algorithm.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Financial and status snapshot of a Grant Program.")
public class ProgramMetadataDTO {

    @Schema(description = "Unique identifier of the grant program",
            example = "3c2a-9e11-4f88-b234-556677889900")
    private UUID programId;

    @Schema(description = "The official name of the grant program",
            example = "Renewable Energy Research Fund 2026")
    private String name;

    @Schema(description = "The current operational status of the program (e.g., OPEN, CLOSED, COMPLETED)",
            example = "CLOSED")
    private String status;

    @Schema(description = "Total approved budget to be distributed among winners",
            example = "500000.00")
    private Double budget;
}