package com.cts.fundtrack.common.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single historical compliance audit entry.
 * <p>
 * Used in audit‑trail and compliance‑history views to display past officer decisions,
 * results, and timestamps related to a grant application.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historical compliance audit record for reporting and audit-trail views.")
public class ComplianceHistoryDTO {

    @Schema(description = "Unique identifier of the compliance audit entry")
    private UUID checkId;

    @Schema(description = "Identifier of the specific report being audited")
    private UUID grantReportId; // <--- ADDED: To link back to the specific submission

    @Schema(description = "Identifier of the associated application (The Anchor)")
    private UUID applicationId;

    @Schema(description = "Identifier of the Compliance Officer who performed the audit")
    private UUID complianceOfficerId; // <--- ADDED: To match your search-by-officer logic

    @Schema(description = "Type of audit performed", example = "DOCUMENT_REVIEW")
    private String auditType;

    @Schema(description = "Final audit outcome", example = "COMPLIANT")
    private String result;

    @Schema(description = "Officer remarks or justification for the audit decision")
    private String notes;

    @Schema(description = "Timestamp of the audit event", example = "2026-03-21T10:15:30Z")
    private String auditDate;
}