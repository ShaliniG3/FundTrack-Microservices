package com.cts.fundtrack.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing application processing statistics for a specific date.
 * <p>
 * This DTO is used to provide a "pivot" view of application statuses (Submitted, Under Review,
 * Approved, Rejected) grouped by the submission date, typically for time-series charts.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Represents application status counts for a specific day")
public class DailyAnalysisDTO {

    /**
     * The date of the analysis in YYYY-MM-DD format.
     */
    @Schema(description = "The date of application activity", example = "2026-03-21")
    private String date;

    /**
     * Total number of applications received with 'SUBMITTED' status on this date.
     */
    @Schema(description = "Count of applications submitted", example = "15")
    private Long submitted;

    /**
     * Total number of applications that moved to or remained in 'UNDER_REVIEW' on this date.
     */
    @Schema(description = "Count of applications currently under review", example = "8")
    private Long underReview;

    /**
     * Total number of applications approved on this date.
     */
    @Schema(description = "Count of applications approved", example = "5")
    private Long approved;

    /**
     * Total number of applications rejected on this date.
     */
    @Schema(description = "Count of applications rejected", example = "2")
    private Long rejected;
}