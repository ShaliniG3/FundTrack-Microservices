package com.cts.fundtrack.common.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO providing a consolidated compliance view for officer dashboards.
 * <p>
 * Used to display key application, program, and reporting details required
 * for monitoring compliance status and prioritizing audit actions.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated compliance summary for dashboard monitoring.")
public class ComplianceDashboardDTO {

    @Schema(
            description = "Unique identifier of the application",
            example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6"
    )
    private UUID applicationId;

    @Schema(
            description = "Name of the applicant or organization",
            example = "Red Cross International"
    )
    private String applicantName;

    @Schema(
            description = "Name of the funding program",
            example = "Emergency Relief Fund 2026"
    )
    private String programName;

    @Schema(
            description = "Current lifecycle status of the application",
            example = "ACTIVE"
    )
    private String applicationStatus;

    @Schema(
            description = "Status of the latest compliance or installment report",
            example = "SUBMITTED"
    )
    private String latestReportStatus;

    @Schema(
            description = "Current installment or milestone number",
            example = "3"
    )
    private Integer currentInstallment;
}