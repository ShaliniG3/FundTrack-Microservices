package com.cts.fundtrack.common.models.enums;

/**
 * Represents the lifecycle states of a funding program in the FundTrack system.
 *
 * <p>Programs follow a linear state progression managed by Program Managers:</p>
 * <pre>
 *   DRAFT → ACTIVE → CLOSED → ARCHIVED
 * </pre>
 * <p>Only {@code ACTIVE} programs accept new grant applications from Applicants.
 * Transitioning out of sequence is prevented by the Program Service lifecycle guards.</p>
 */
public enum ProgramStatus {

    /** The program is being configured and is not yet visible or open to applicants. */
    DRAFT,

    /** The program is published and accepting grant applications. */
    ACTIVE,

    /** The program's application window has ended; no new submissions are accepted. */
    CLOSED,

    /** The program has been fully processed and archived for historical record-keeping. */
    ARCHIVED
}

