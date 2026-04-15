package com.cts.fundtrack.common.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Categorizes the types of entities within the FundTrack system for auditing purposes.
 */
@Schema(description = "The specific domain entity type associated with an audit log or system event")
public enum EntityType {

    @Schema(description = "A grant program or funding opportunity")
    PROGRAM,

    @Schema(description = "Criteria defined to determine applicant eligibility for a program")
    ELIGIBILITY_RULE,

    @Schema(description = "A specific document type required by a program for submission")
    REQUIRED_DOCUMENT,

    @Schema(description = "A registered system user (Applicant, Reviewer, or Admin)")
    USER,

    @Schema(description = "A financial transaction record representing a successful fund transfer")
    PAYMENT,

    @Schema(description = "Official evidence/metrics submitted by a grantee")
    GRANT_REPORT,


    @Schema(description = "A scheduled or historical payout installment tied to an application")
    DISBURSEMENT,

    @Schema(description = "A request for funding submitted by an applicant for a specific program")
    APPLICATION,

    @Schema(description = "A system-generated record of an action performed by a user")
    AUDIT_LOG,
    
    FINANCE,
    
    DECISION,
    
    REVIEW
}