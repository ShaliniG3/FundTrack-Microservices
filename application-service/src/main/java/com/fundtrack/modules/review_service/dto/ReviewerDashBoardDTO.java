package com.fundtrack.modules.review_service.dto;

import com.fundtrack.modules.review_service.models.Review;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ReviewerDashBoardDTO {
    private int count;
    private List<Review> reviews;
}