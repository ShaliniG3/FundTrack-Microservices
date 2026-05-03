package com.cts.fundtrack.common.dto;

import lombok.*;
import java.util.UUID;

/**
 * Data Transfer Object representing a reviewer's formal recommendation on a grant application.
 *
 * <p>Submitted alongside a {@link ReviewRequestDTO} by a Reviewer after completing their
 * evaluation. The recommendation is a non-binding advisory opinion that feeds into the
 * Approver's final decision workflow and is surfaced in the
 * {@link ApplicationDecisionDetailsDTO}.</p>
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendationDTO {

    /** Unique identifier of this recommendation record. */
    private UUID recommendationId;

    /**
     * The reviewer's advisory verdict.
     * Expected values: {@code "RECOMMENDED"} or {@code "NOT_RECOMMENDED"}.
     */
    private String recommendationStatus;

    /**
     * Free-text explanation provided by the Reviewer supporting their recommendation,
     * highlighting strengths, weaknesses, or risk factors identified during evaluation.
     */
    private String justification;
}