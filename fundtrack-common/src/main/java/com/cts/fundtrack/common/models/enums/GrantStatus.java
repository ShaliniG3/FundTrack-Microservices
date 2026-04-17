package com.cts.fundtrack.common.models.enums;

/**
 * Represents the processing status of a grant application as used by the Analytics Service.
 *
 * <p>This enum is used in analytics aggregation to categorize applications by their
 * current workflow stage. It mirrors the active states of {@link ApplicationStatus}
 * but is scoped to the analytics domain for status-distribution and daily-trend queries.</p>
 */
public enum GrantStatus {

    /** The application has been submitted and is pending review assignment. */
    SUBMITTED,

    /** The application is actively under evaluation by Reviewers. */
    UNDER_REVIEW,

    /** The application has been approved; grant disbursement is authorized. */
    APPROVED,

    /** The application has been rejected; no disbursement will occur. */
    REJECTED
}


