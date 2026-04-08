package com.cts.fundtrack.dgcs.dto.grantreportdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;
/**
 * DTO representing the metadata required to submit a grant progress report.
 * <p>
 * Used as the JSON component of a multipart submission, containing descriptive
 * and quantitative details about the applicant's project progress.
 * </p>
 */
@Data
@Schema(description = "Request body for submitting grant progress details and performance metrics.")
public class GrantReportRequestDTO {

    @NotNull(message = "Application ID is required to link the report.")
    @Schema(
            description = "Unique identifier of the application associated with this report",
            example = "550e8400-e29b-41d4-Aa716-446655440000"
    )
    private UUID applicationId;

    @NotBlank(message = "Report scope cannot be empty.")
    @Size(min = 10, max = 2000, message = "Scope must be between 10 and 2000 characters.")
    @Schema(
            description = "Narrative summary of progress made during the reporting period",
            example = "Completed Phase 2 of the rural electrification initiative, covering 4 additional villages."
    )
    private String scope;

    @NotBlank(message = "Performance metrics are required.")
    @Schema(
            description = "Key performance indicators or measurable outputs achieved",
            example = "Households connected: 450; Technical staff trained: 15"
    )
    private String metrics;
}