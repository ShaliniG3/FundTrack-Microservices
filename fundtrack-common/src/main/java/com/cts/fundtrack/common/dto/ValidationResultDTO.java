package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing the outcome of a single eligibility rule evaluation
 * performed against an application's data during the validation phase.
 *
 * <p>Each instance corresponds to one {@link EligibilityRuleDTO} expression being
 * evaluated by the SpEL engine. The results are aggregated and stored alongside the
 * application so that Reviewers and Approvers can see exactly which rules passed or
 * failed and why.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDTO {

    /**
     * Human-readable name of the eligibility rule that was evaluated
     * (e.g., {@code "Income Threshold Check"}, {@code "Age Eligibility Check"}).
     */
    private String ruleName;

    /**
     * String outcome of the evaluation. Expected values: {@code "PASSED"} or {@code "FAILED"}.
     */
    private String result;

    /**
     * Boolean convenience flag mirroring whether {@code result} equals {@code "PASSED"}.
     * Consumed by the frontend to render pass/fail indicators without string comparison.
     */
    private boolean passed;

    /**
     * Human-readable explanation of the evaluation outcome
     * (e.g., {@code "Income is within the allowed range."} or
     * {@code "Income exceeds the maximum threshold of 50,000."}).
     */
    private String message;

    /** Timestamp recording when this validation check was executed. */
    private LocalDateTime validatedAt;
}