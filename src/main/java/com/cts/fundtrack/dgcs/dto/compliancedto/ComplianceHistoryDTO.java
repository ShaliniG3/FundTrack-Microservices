package com.cts.fundtrack.dgcs.dto.compliancedto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing a historical compliance audit entry.
 * <p>
 * This model serves the audit trail requirements of the system, providing
 * a read-only snapshot of past officer decisions, verification types,
 * and justifications. It is primarily utilized in the Compliance History
 * views to ensure transparency throughout the grant lifecycle.
 * </p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historical compliance audit record for reporting and audit-trail views.")
public class ComplianceHistoryDTO {

    @Schema(description = "Unique identifier of the compliance audit entry",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID checkId;

    @Schema(description = "Identifier of the specific report being audited",
            example = "74d2d68a-ed85-43a0-bd0c-d4402f51822e")
    private UUID grantReportId;

    @Schema(description = "Identifier of the associated application anchor",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationId;

    @Schema(description = "Identifier of the Compliance Officer who performed the audit",
            example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID complianceOfficerId;

    @Schema(description = "Type of audit performed",
            example = "DOCUMENT_REVIEW")
    private String auditType;

    @Schema(description = "Final audit outcome",
            example = "COMPLIANT")
    private String result;

    @Schema(description = "Officer remarks or justification for the audit decision",
            example = "All financial receipts match the submitted project scope.")
    private String notes;

    @Schema(description = "Timestamp of the audit event",
            example = "2026-03-21T10:15:30Z")
    private String auditDate;
}