package com.cts.fundtrack.common.dto;

import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendationDTO {
    private UUID recommendationId;
    private String recommendationStatus; // e.g., RECOMMENDED, NOT_RECOMMENDED
    private String justification;
}