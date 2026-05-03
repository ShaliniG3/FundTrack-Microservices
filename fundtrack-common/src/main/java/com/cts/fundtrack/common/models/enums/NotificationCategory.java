package com.cts.fundtrack.common.models.enums;
/**
 * Defines the functional categories for system notifications.
 * This Enum is used by the template engine to select the appropriate
 * message format and by the frontend to filter or style alerts.
 */
public enum NotificationCategory {

    /** * Initial application submission.
     * Triggered when a user first submits a funding request.
     */
    SUBMITTED,

    /** * Internal review process.
     * Triggered when a reviewer begins evaluating an application.
     */
    UNDER_REVIEW,

    /** * Successful application status.
     * Triggered when an application is officially approved.
     */
    APPROVAL,

    /** * Financial transfer status.
     * Triggered when funds have been successfully sent to the applicant.
     */
    DISBURSEMENT,

    /** * Regulatory or documentation requests.
     * Triggered when additional documents or compliance checks are required.
     */
    COMPLIANCE,

    /**
     * General manual messages.
     * Used as a default for simple notifications that do not use templates.
     */
    GENERAL, REJECTED, APPLICATION, ACCEPTED,
}