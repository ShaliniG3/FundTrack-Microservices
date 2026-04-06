package com.fundtrack.modules.review_service.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecommendationDTO {
    private UUID id;
    private UUID applicationId;
    private UUID reviewerId;
    private String decision; // E.g., "Recommended", "Rejected"
    private String notes;
    private LocalDate date;
}