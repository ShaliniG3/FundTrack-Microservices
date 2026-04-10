package com.cts.fundtrack.dgcs.dto.grantreportdto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.util.UUID;

/**
 * Data Transfer Object representing the metadata required to submit a grant progress report.
 * <p>
 * This DTO serves as the structured JSON component of a multipart submission,
 * encapsulating qualitative narratives and quantitative performance metrics.
 * </p>
 */

@Data
@Schema(description = "Request model for project progress documentation and performance data.")
public class GrantReportRequestDTO {

    @NotNull(message = "Application ID is required to link the report.")
    @Schema(
            description = "The unique identifier of the application which associated with this report.",
            example = "550e8400-e29b-41d4-Aa716-446655440000"
    )
    private UUID applicationId;

    @NotBlank(message = "Report scope cannot be empty.")
    @Size(min = 10, max = 2000, message = "Scope must be between 10 and 2000 characters.")
    @Schema(
            description = "Formal narrative detailing milestones achieved during the reporting period.",
            example = "Successfully completed infrastructure deployment for Phase 2."
    )
    private String scope;

    @NotBlank(message = "Performance metrics are required.")
    @Schema(
            description = "Quantitative indicators and measurable project outputs.",
            example = "Households connected: 450; Technical staff trained: 15"
    )
    private String metrics;
}