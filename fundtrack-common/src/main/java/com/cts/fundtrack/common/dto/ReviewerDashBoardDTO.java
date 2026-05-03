package com.cts.fundtrack.common.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the Reviewer's dashboard summary.
 *
 * <p>Provides a Reviewer with the total count of reviews they have completed and
 * the full list of those review records. Using {@link ReviewDTO} instead of the
 * internal Review entity maintains module isolation between the Review Service
 * and any consuming service or frontend.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerDashBoardDTO {

    /** Total number of reviews completed by this reviewer. */
    private int count;

    /** Detailed list of each review submitted by this reviewer. */
    private List<ReviewDTO> reviews;
}