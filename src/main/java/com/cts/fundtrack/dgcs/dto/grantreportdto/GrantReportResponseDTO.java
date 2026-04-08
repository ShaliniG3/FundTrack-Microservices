package com.cts.fundtrack.dgcs.dto.grantreportdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO representing the response returned after a grant report is submitted
 * or retrieved for review. Provides summary information required for
 * applicant dashboards and compliance audit workflows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model containing summary details of a submitted or retrieved grant report.")
public class GrantReportResponseDTO {

    @Schema(
            description = "Unique identifier of the grant report",
            example = "e1f2g3h4-i5j6-7k8l-9m0n-o1p2q3r4s5t6"
    )
    private UUID reportId;

    @Schema(
            description = "Identifier of the application associated with this report",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID applicationId;

    @Schema(
            description = "Narrative summary of project progress submitted in the report",
            example = "Completed Phase 2 of infrastructure setup including 4 new community centers."
    )
    private String scope;

    @Schema(
            description = "Quantifiable performance metrics or KPIs reported",
            example = "New households connected: 350; Volunteers trained: 22"
    )
    private String metrics;

    @Schema(
            description = "Current status of the report",
            example = "SUBMITTED"
    )
    private String status;

    @Schema(
            description = "Storage path or URL of the uploaded PDF document",
            example = "/files/reports/proof_8734.pdf"
    )
    private String pdfPath;

    @Schema(
            description = "Informational message confirming the result of the operation",
            example = "Report successfully submitted and queued for review."
    )
    private String message;
}