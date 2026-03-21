package com.fundtrack.analytics_service.dto;

import com.fundtrack.analytics_service.model.external.GrantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the count of applications for a specific status.
 * <p>
 * This DTO is primarily used in distribution analytics to show a breakdown of how many
 * applications are in each stage (e.g., SUBMITTED, APPROVED, REJECTED) within a program.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Represents a single status category and the total number of applications assigned to it")
public class StatusCountDTO {

    /**
     * The lifecycle state of the application.
     * Common values include: SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED.
     */
    @Schema(description = "The specific application status", example = "APPROVED")
    private GrantStatus status;

    /**
     * The total number of applications currently holding this status.
     */
    @Schema(description = "The count of applications with this status", example = "42")
    private Long count;

}