package com.cts.fundtrack.dgcs.dto.compliancedto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing applicant-level compliance information for dashboards.
 * <p>
 * This model serves as a consolidated view for the Compliance Workbench,
 * aggregating application status, program metadata, and reporting progress.
 * It enables Compliance Officers to efficiently monitor the fiduciary
 * standing of various grant recipients.
 * </p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compliance summary model used in dashboards and audit workflows.")
public class ApplicantComplianceDTO {

    @Schema(description = "Unique identifier of the grant application",
            example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
    private UUID applicationId;

    @Schema(description = "Name of the applicant or organization",
            example = "Global Health Initiative")
    private String applicantName;

    @Schema(description = "Name of the grant program",
            example = "Community Development Grant 2026")
    private String programName;

    @Schema(description = "Current status of the application",
            example = "ACTIVE")
    private String applicationStatus;

    @Schema(description = "Status of the applicant's latest submitted report",
            example = "SUBMITTED")
    private String latestReportStatus;

    @Schema(description = "Current installment or milestone number",
            example = "2")
    private Integer currentInstallment;
}
