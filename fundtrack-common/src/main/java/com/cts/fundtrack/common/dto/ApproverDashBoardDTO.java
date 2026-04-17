package com.cts.fundtrack.common.dto;

import lombok.*;
import java.util.List;

/**
 * Data Transfer Object representing the Approver's dashboard summary.
 *
 * <p>Provides an Approver with a count of decisions they have made and the
 * full list of those decision records, enabling them to review their approval
 * history and current workload at a glance.</p>
 */
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ApproverDashBoardDTO {

    /** Total number of decisions recorded for this approver. */
    private int count;

    /** Detailed list of each decision (APPROVED or REJECTED) made by this approver. */
    private List<DecisionDTO> decisions;
}