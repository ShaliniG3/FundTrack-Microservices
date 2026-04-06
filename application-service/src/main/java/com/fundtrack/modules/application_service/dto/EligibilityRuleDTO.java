package com.fundtrack.modules.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Requirement 4.2: DTO for capturing eligibility rules defined in the
 * Grant Program & Eligibility Management module.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRuleDTO {

    private UUID ruleId;           // Matches RuleID in the ER diagram
    private UUID programId;        // Links the rule to a specific Grant Program [cite: 41, 92]
    private String ruleDescription; // Human-readable description of the rule
    private String ruleExpression;  // The logic string used by the Validation Engine
}