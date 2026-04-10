package com.cts.fundtrack.dgcs.dto.compliancedto;

import com.cts.fundtrack.dgcs.model.enums.ComplianceStatus;
import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing the result of a formal compliance audit.
 * <p>
 * This model is returned after a Compliance Officer records a verdict on a grant report.
 * It synchronizes the audit record details with the updated status of the
 * associated report, providing a unified confirmation for the frontend.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object confirming the audit verdict results and report state synchronization.")
public class ComplianceCheckResponseDTO {

    @Schema(description = "Unique identifier for the specific audit check record",
            example = "c0a80121-7ac0-11ed-a1eb-0242ac120002")
    private UUID checkId;

    @Schema(description = "Identifier of the grant report that was audited",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID grantReportId;

    @Schema(description = "The official verdict reached by the officer",
            example = "COMPLIANT")
    private ComplianceStatus status;

    @Schema(description = "The new synchronized status of the grant report",
            example = "APPROVED")
    private GrantReportStatus reportStatus;

    @Schema(description = "Timestamp when the audit was finalized",
            example = "2026-04-09T14:30:00Z")
    private Instant auditDate;

    @Schema(description = "Justification or feedback provided by the compliance officer",
            example = "Financial documentation verified against program guidelines.")
    private String remarks;
}