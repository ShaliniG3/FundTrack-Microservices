package com.cts.fundtrack.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


/**
 * Data Transfer Object for submitting a review evaluation on a grant application.
 *
 * <p>Sent by a Reviewer to the Review Service to record their assessment of an
 * application that is in {@code SUBMITTED} status. The request carries both the
 * quantitative score and qualitative comments, as well as a formal
 * {@link RecommendationDTO} representing the reviewer's advisory verdict.</p>
 */
@Data
@NoArgsConstructor
public class ReviewRequestDTO {

    /** Unique identifier of the application being reviewed. */
    private UUID applicationId;

    /** Unique identifier of the Reviewer submitting this evaluation. */
    private UUID reviewerId;

    /**
     * Numeric quality score assigned by the Reviewer.
     * Expected range: 0 – 100. Validated by the service layer.
     */
    private Integer score;

    /** Qualitative feedback, observations, and risk notes recorded by the Reviewer. */
    private String comments;

    /**
     * The Reviewer's formal advisory recommendation (RECOMMENDED or NOT_RECOMMENDED)
     * along with a written justification.
     */
    private RecommendationDTO recommendation;
}