package com.cts.fundtrack.common.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared DTO for the Reviewer Dashboard.
 * Uses ReviewDTO instead of the Review Entity to maintain module isolation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerDashBoardDTO {
    
    private int count;
    
    private List<ReviewDTO> reviews;
}