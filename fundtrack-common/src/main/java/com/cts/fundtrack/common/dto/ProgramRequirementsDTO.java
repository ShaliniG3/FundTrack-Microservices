package com.cts.fundtrack.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object bundling all validation requirements for a specific funding program.
 *
 * <p>Fetched by the Application Service before validating a submission. It provides
 * two complementary lists that drive the two-phase validation pipeline:</p>
 * <ol>
 *   <li><b>Document checklist</b> — verifies that all required document types have been uploaded.</li>
 *   <li><b>Eligibility rules</b> — evaluates SpEL expressions against the applicant's data
 *       to determine programmatic eligibility.</li>
 * </ol>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramRequirementsDTO {

    /** Unique identifier of the funding program. */
    private UUID programId;

    /** Human-readable name of the funding program. */
    private String programName;

    /**
     * List of document type keys that must be present in the applicant's upload set
     * (e.g., {@code "ID_PROOF"}, {@code "INCOME_CERTIFICATE"}).
     * The Application Service checks that each key exists in the submission's document map.
     */
    private List<String> requiredDocuments;

    /**
     * List of eligibility rules whose {@code ruleExpression} fields contain SpEL expressions
     * (e.g., {@code "income < 50000"}). The Application Service evaluates each expression
     * against the applicant's parsed {@code applicationData} to determine eligibility.
     */
    private List<EligibilityRuleDTO> rules;
}