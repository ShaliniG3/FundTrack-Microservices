package com.fundtrack.modules.decision_service.dto;

import com.fundtrack.modules.decision_service.models.Decision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApproverDashBoardDTO {
    private int count;
    private List<Decision> decisions;
}