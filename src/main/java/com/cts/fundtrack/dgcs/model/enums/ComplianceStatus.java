package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration representing the regulatory and policy adherence status of an applicant.
 * <p>
 * This status acts as a financial gatekeeper, determining eligibility for fund
 * disbursement based on legal, programmatic, and fiduciary requirements.
 * </p>
 */
@Schema(description = "Regulatory adherence status governing fund disbursement eligibility")
public enum ComplianceStatus {

    @Schema(description = "Applicant is currently undergoing verification; eligibility is not yet determined.")
    PENDING,

    @Schema(description = "Applicant meets all fiduciary and legal criteria; disbursement is authorized.")
    COMPLIANCE,

    @Schema(description = "Applicant fails to meet one or more regulatory requirements; disbursement is restricted.")
    NON_COMPLIANT,

    @Schema(description = "Previous compliance data has expired or requires re-validation.")
    FLAGGED_FOR_REVIEW
}