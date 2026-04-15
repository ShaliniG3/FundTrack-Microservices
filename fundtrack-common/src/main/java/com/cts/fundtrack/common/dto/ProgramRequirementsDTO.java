package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramRequirementsDTO {
    
    private UUID programId;
    private String programName;
    
    /**
     * The Checklist:
     * Simple strings representing the names of documents required (e.g., "ID_PROOF").
     * Your Application Service will check if these keys exist in the upload map.
     */
    private List<String> requiredDocuments; 
    
    /**
     * The Formulas:
     * Full objects containing the 'ruleExpression' (like "income < 50000").
     * Your performValidation() method uses these to drive the SpelExpressionParser.
     */
    private List<EligibilityRuleDTO> rules; 
}