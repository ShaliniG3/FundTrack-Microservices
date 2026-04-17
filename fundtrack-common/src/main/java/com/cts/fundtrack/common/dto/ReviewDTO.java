package com.cts.fundtrack.common.dto;

import lombok.*;
import java.util.UUID;

/**
 * Data Transfer Object representing a reviewer's evaluation of a grant application.
 *
 * <p>Created by the Review Service when a Reviewer submits their assessment. The
 * review record — particularly the {@code score} and {@code comments} — is surfaced
 * in the {@link ApplicationDecisionDetailsDTO} so that Approvers have full context
 * before rendering a final decision.</p>
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDTO {

    /** Unique identifier of this review record. */
    private UUID reviewId;

    /** Unique identifier of the application that was reviewed. */
    private UUID applicationId;

    /** Display name of the Reviewer who submitted this evaluation. */
    private String reviewerName;

    /**
     * Numeric score assigned by the Reviewer reflecting the application's overall quality.
     * Expected range: 0 – 100.
     */
    private Integer score;

    /** Qualitative feedback and observations recorded by the Reviewer. */
    private String comments;
}