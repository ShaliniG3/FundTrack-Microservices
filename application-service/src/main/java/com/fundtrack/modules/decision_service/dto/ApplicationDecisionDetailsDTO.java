package com.fundtrack.modules.decision_service.dto;

import com.fundtrack.modules.application_service.dto.ApplicationResponseDTO;
import com.fundtrack.modules.review_service.models.Recommendation;
import com.fundtrack.modules.review_service.models.Review;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationDecisionDetailsDTO {
    private ApplicationResponseDTO applicationDetails;
    private Review review;
    private Recommendation recommendation;
}