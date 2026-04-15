package com.cts.fundtrack.common.dto;

import lombok.*;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ApplicationDecisionDetailsDTO {
    private ApplicationResponseDTO applicationDetails;
    private ReviewDTO review;           // Uses the DTO from Step 1
    private RecommendationDTO recommendation; // Uses the DTO from Step 1
}