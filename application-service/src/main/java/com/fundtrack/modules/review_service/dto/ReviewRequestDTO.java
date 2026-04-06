package com.fundtrack.modules.review_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@NoArgsConstructor
public class ReviewRequestDTO {
    private UUID applicationId;
    private UUID reviewerId;
    private Integer score;
    private String comments;
    
    // Instead of a single String, we call the full DTO
    private RecommendationDTO recommendation;
}