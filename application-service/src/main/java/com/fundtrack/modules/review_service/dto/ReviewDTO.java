package com.fundtrack.modules.review_service.dto;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDate;

@Data
public class ReviewDTO {
    private UUID id;
    private UUID applicationId;
    private UUID reviewerId;
    private Integer score;
    private String comments;
    private LocalDate date;
}