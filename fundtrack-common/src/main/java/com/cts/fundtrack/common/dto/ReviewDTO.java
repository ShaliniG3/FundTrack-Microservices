package com.cts.fundtrack.common.dto;

import lombok.*;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDTO {
    private UUID reviewId;
    private UUID applicationId;
    private String reviewerName;
    private Integer score;
    private String comments;
}