package com.cts.fundtrack.common.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Eligibility Rules.
 * Aligned with the 'eligibility_rule' table in the ER diagram.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityRuleDTO {

    /**
     * Unique identifier for the rule. 
     * If provided, the system treats this as an update to an existing rule.
     */
    private UUID ruleId; 

    /**
     * Matches 'rule_description' in the entity.
     */
    @NotBlank(message = "Rule description is required.")
    private String ruleDescription;

    /**
     * Matches 'rule_expression' in the entity.
     * Represented as 'Status_Criterion' in the ER diagram.
     */
    @NotBlank(message = "Rule expression (criterion) is required.")
    private String ruleExpression;
}

