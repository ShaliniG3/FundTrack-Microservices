package com.cts.fundtrack.common.models.enums;

/**
 * Represents the document verification status for a grant application's supporting evidence.
 *
 * <p>Used by the Application Service to track whether an applicant's uploaded documents
 * have been reviewed and approved by an authorized officer. Only applications with
 * {@code DOCUMENT_APPROVED} status are eligible to proceed through the review pipeline.</p>
 */
public enum VerificationStatus {

    /** Supporting documents have been uploaded and are pending officer review. */
    SUBMITTED,

    /** Submitted documents have been verified and accepted by an authorized reviewer. */
    DOCUMENT_APPROVED,

    /** Submitted documents were found to be invalid or insufficient and were rejected. */
    DOCUMENT_REJECTED
}