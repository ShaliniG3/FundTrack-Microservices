package com.fundtrack.analytics_service.dto.eligibilitydto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for managing grant eligibility rules.
 * <p>
 * This DTO aligns with the underlying {@code eligibility_rule} persistence model.
 * it is used to define the logical criteria (expressions) that an application
 * must satisfy to be considered for funding.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Configuration model for a specific eligibility criterion or business rule")
public class EligibilityRuleDTO {

    /**
     * Unique identifier for the eligibility rule.
     * <p>Optional for creation; mandatory for updating existing rules.</p>
     */
    @Schema(description = "Unique ID of the eligibility rule", example = "7a1b2c3d-4e5f-6g7h-8i9j-0k1l2m3n4o5p")
    private UUID ruleId;

    /**
     * Human-readable description of the eligibility requirement.
     */
    @NotBlank(message = "Rule description is required.")
    @Schema(description = "Human-readable explanation of the rule",
            example = "Applicant must be a registered non-profit organization.")
    private String ruleDescription;

    /**
     * The technical logical expression used by the rules engine.
     * <p>Represents the 'Status_Criterion' used to evaluate application data.</p>
     */
    @NotBlank(message = "Rule expression (criterion) is required.")
    @Schema(description = "Technical logic or expression for the rule engine",
            example = "applicant.type == 'NGO' && applicant.years_active >= 3")
    private String ruleExpression;
}