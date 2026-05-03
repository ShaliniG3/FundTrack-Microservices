package com.cts.fundtrack.common.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the result of an automated or manual document validation check.
 * <p>
 * This DTO tracks the execution of specific business rules (e.g., OCR Data Matching,
 * Expiry Date Verification) against uploaded files and records the pass/fail outcome.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a validation rule execution and its resulting status")
public class ValidationDTO {

    /**
     * Unique identifier for the specific validation record.
     */
    @Schema(description = "The unique ID of the validation entry", example = "e5f6g7h8-i9j0-k1l2-m3n4-o5p6q7r8s9t0")
    private UUID validationId;

    /**
     * The name or code of the validation rule that was executed.
     * <p>Examples: OCR_IDENTITY_MATCH, DOCUMENT_EXPIRY_CHECK, FILE_CORRUPTION_SCAN.</p>
     */
    @Schema(description = "The name of the business rule applied", example = "OCR_IDENTITY_MATCH")
    private String ruleName;

    /**
     * The outcome of the validation check.
     * <p>Expected values: PENDING, PASSED, FAILED, WARNING.</p>
     */
    @Schema(description = "The status or result of the validation", example = "PENDING")
    private String result;

    /**
     * Detailed feedback or error message regarding the validation result.
     * <p>Provides context if a rule fails (e.g., 'Name on ID does not match application').</p>
     */
    @Schema(description = "Feedback message or error details from the validation engine",
            example = "Scanning in progress. Waiting for OCR provider response.")
    private String message;

    /**
     * The timestamp indicating when the validation check was performed.
     */
    @Schema(description = "The date and time the validation was executed")
    private Instant checkedDate;
}