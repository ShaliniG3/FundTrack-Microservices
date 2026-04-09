package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration representing the formal lifecycle states of a grant progress report.
 * <p>
 * These states govern the workflow from initial recipient submission through
 * compliance audit and final administrative validation.
 * </p>
 */

@Schema(description = "Formal lifecycle states of a grant report or accountability document")
public enum GrantReportStatus {

    @Schema(description = "The report has been formally filed by the recipient and is awaiting initial processing.")
    SUBMITTED,

    @Schema(description = "The report is currently undergoing a compliance audit or administrative review.")
    UNDER_REVIEW,

    @Schema(description = "The report has been validated and accepted as meeting all programmatic requirements.")
    APPROVED,

    @Schema(description = "The report contains inaccuracies or omissions and has been sent back to the recipient for correction.")
    RETURNED_FOR_REVISION,

    @Schema(description = "The report has been permanently declined due to non-compliance or failure to meet grant conditions.")
    REJECTED
}