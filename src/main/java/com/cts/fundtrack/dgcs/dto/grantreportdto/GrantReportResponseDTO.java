package com.cts.fundtrack.dgcs.dto.grantreportdto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing the summarized view of a grant progress report.
 * <p>
 * This DTO provides a read-only snapshot of the report's metadata, including
 * performance KPIs, submission status, and references to supporting documentation.
 * </p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model containing summary details of a submitted or retrieved grant report.")

public class GrantReportResponseDTO {

    @Schema(
            description = "Internal unique identifier for the specific report submission.",
            example = "e1f2g3h4-i5j6-7k8l-9m0n-o1p2q3r4s5t6"
    )
    private UUID grantReportId;

    @Schema(
            description = "The unique identifier of the parent grant application.",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID applicationId;

    @Schema(
            description = "Detailed narrative of milestones achieved during this reporting cycle.",
            example = "Completed Phase 2 of infrastructure setup including 4 new community centers."
    )
    private String scope;

    @Schema(
            description = "Quantitative performance indicators and measurable outputs.",
            example = "New households connected: 350; Volunteers trained: 22"
    )
    private String metrics;

    @Schema(
            description = "The current lifecycle state of the report (e.g., SUBMITTED, REJECTED, APPROVED).",
            example = "SUBMITTED"
    )
    private String status;

    @Schema(
            description = "The secure URI or reference key for the uploaded PDF evidence.",
            example = "https://storage.provider.com/reports/proof_8734.pdf"
    )
    private String documentUrl;

    @Schema(
            description = "System-generated acknowledgment confirming the operation result.",
            example = "Report successfully submitted and queued for review."
    )
    private String acknowledgment;
}