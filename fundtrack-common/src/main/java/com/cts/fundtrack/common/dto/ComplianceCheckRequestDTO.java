package com.cts.fundtrack.common.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for submitting a compliance audit verdict for a grant report.
 * <p>
 * Contains the audit decision, remarks, and audit type, with validation rules
 * ensuring integrity and correctness of officer-submitted data.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for submitting a compliance audit decision on a grant report.")
public class ComplianceCheckRequestDTO {

    @NotNull(message = "Report ID is required for auditing")
    @Schema(description = "Identifier of the grant report being audited")
    private UUID grantReportId;

    @NotNull(message = "Officer ID is required")
    @Schema(description = "Identifier of the Compliance Officer performing the audit")
    private UUID complianceOfficerId; // <--- ADDED THIS

    @NotBlank(message = "Audit result status is mandatory")
    @Pattern(
            regexp = "COMPLIANCE|NON_COMPLIANT",
            message = "Status must be either COMPLIANCE or NON_COMPLIANT"
    )
    @Schema(allowableValues = {"COMPLIANCE", "NON_COMPLIANT"})
    private String status;

    @NotBlank(message = "Audit comments are required for transparency")
    private String comments;

    @NotBlank(message = "Audit type is required")
    @Schema(example = "FINANCIAL_VERIFICATION")
    private String type;
}