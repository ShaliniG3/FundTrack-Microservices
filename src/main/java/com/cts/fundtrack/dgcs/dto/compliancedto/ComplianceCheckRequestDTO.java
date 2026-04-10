package com.cts.fundtrack.dgcs.dto.compliancedto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for submitting a compliance audit verdict.
 * <p>
 * This DTO captures the formal decision made by a Compliance Officer regarding
 * a specific grant report. The status provided here acts as the final logical
 * gate that determines whether the Finance Module is authorized to release
 * the next scheduled disbursement installment.
 * </p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for submitting a compliance audit decision on a grant report.")
public class ComplianceCheckRequestDTO {

    @NotNull(message = "Report ID is required for auditing")
    @Schema(description = "Identifier of the grant report being audited",
            example = "74d2d68a-ed85-43a0-bd0c-d4402f51822e")
    private UUID grantReportId;

    @NotNull(message = "Officer ID is required")
    @Schema(description = "Identifier of the Compliance Officer performing the audit",
            example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID complianceOfficerId;

    @NotBlank(message = "Audit result status is mandatory")
    @Pattern(
            regexp = "COMPLIANCE|NON_COMPLIANT",
            message = "Status must be either COMPLIANCE or NON_COMPLIANT"
    )
    @Schema(description = "The adherence verdict (Must match the defined pattern)",
            allowableValues = {"COMPLIANCE", "NON_COMPLIANT"},
            example = "COMPLIANCE")
    private String status;

    @NotBlank(message = "Audit comments are required for transparency")
    @Schema(description = "Detailed justification for the compliance verdict",
            example = "Report documentation verified. All milestones achieved as per Phase 1 requirements.")
    private String comments;

    @NotBlank(message = "Audit type is required")
    @Schema(description = "The specific category of compliance check performed",
            example = "FINANCIAL")
    private String type;
}