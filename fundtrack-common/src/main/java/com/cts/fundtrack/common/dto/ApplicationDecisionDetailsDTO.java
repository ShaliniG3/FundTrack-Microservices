package com.cts.fundtrack.common.dto;

import lombok.*;

/**
 * Data Transfer Object aggregating all decision-related data for a single grant application.
 *
 * <p>Used by the Approver workflow to present a consolidated view of an application
 * alongside the reviewer's score/comments and the reviewer's formal recommendation,
 * so that the Approver can make a fully informed APPROVED or REJECTED decision.</p>
 */
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ApplicationDecisionDetailsDTO {

    /** Full application details including program, applicant, status, and documents. */
    private ApplicationResponseDTO applicationDetails;

    /** The reviewer's score and comments submitted during the review phase. */
    private ReviewDTO review;

    /** The reviewer's formal recommendation (RECOMMENDED or NOT_RECOMMENDED) with justification. */
    private RecommendationDTO recommendation;
}