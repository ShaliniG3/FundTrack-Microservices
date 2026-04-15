package com.cts.fundtrack.common.dto;

import lombok.*;
import java.util.List;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ApproverDashBoardDTO {
    private int count;
    private List<DecisionDTO> decisions; // Uses the DTO from Step 1
}