package com.fundtrack.modules.decision_service.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DecisionRequestDTO {
    private UUID applicationId;
    private UUID approverId;
    private String decision; // "APPROVED" or "REJECTED"
    private String notes;
}