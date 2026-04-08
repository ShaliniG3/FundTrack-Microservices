package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tracks the status of post-award financial or progress reports
 * submitted by grant recipients.
 */
@Schema(description = "The lifecycle states of a grant report or accountability document")
public enum GrantReportStatus {

    @Schema(description = "The report has been reviewed and validated by the administration")
    APPROVED,

    @Schema(description = "The recipient has filed the report and it is awaiting review")
    SUBMITTED,

    @Schema(description = "A required report is due but has not yet been filed by the recipient")
    PENDING,

    @Schema(description = "The report was sent back due to missing information or inaccuracies")
    REJECTED,

    @Schema(description = "The reporting requirement for this period is fully finalized and closed")
    COMPLETED
}