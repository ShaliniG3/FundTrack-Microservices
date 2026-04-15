package com.cts.fundtrack.common.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionDTO {
    
    private UUID decisionId;
    
    private UUID applicationId;
    
    private UUID approverId;
    
    /**
     * Aligned with service logic: APPROVED or REJECTED
     */
    private String decision; 
    
    /**
     * Aligned with service logic: The justification text
     */
    private String notes; 
    
    /**
     * The date the decision was finalized
     */
    private LocalDate date;
}