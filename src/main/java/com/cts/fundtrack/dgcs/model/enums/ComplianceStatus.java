package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Indicates whether an applicant meets all legal and regulatory requirements
 * for receiving funds at the time of disbursement.
 */
@Schema(description = "Verification status of an applicant's adherence to grant policies and legal requirements")
public enum ComplianceStatus {

    @Schema(description = "Applicant meets all requirements; funds can be safely released")
    COMPLIANCE,

    @Schema(description = "Applicant has failed a check; disbursements must be halted immediately")
    NON_COMPLIANT
}