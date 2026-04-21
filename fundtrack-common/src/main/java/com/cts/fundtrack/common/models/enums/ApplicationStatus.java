package com.cts.fundtrack.common.models.enums;

/**
 * Represents the lifecycle states of a grant application within the FundTrack system.
 *
 * <p>Applications follow this state progression:</p>
 * <pre>
 *   DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED | ACCEPTED | REJECTED
 * </pre>
 */
public enum ApplicationStatus {

    /** The application has been saved but not yet officially submitted by the applicant. */
    DRAFT,

    /** The application has been submitted by the applicant and is awaiting reviewer assignment. */
    SUBMITTED,

    /** The application is actively being evaluated by one or more Reviewers. */
    UNDER_REVIEW,

    /** The application has been approved by an Approver and grant funding will be disbursed. */
    APPROVED,

    /** The application has been accepted by an Approver; grant offer is confirmed and pending disbursement. */
    ACCEPTED,

    /** The application has been rejected by an Approver; no funding will be disbursed. */
    REJECTED
}