package com.cts.fundtrack.common.dto;

import lombok.Data;
import java.util.UUID;

/**
 * Data Transfer Object for submitting an approval decision on a grant application.
 *
 * <p>Sent by an Approver to the Decision Service to record a final verdict on an
 * application that has completed the review phase. The application must be in
 * {@code UNDER_REVIEW} status before a decision can be accepted.</p>
 */
@Data
public class DecisionRequestDTO {

    /** Unique identifier of the application receiving the decision. */
    private UUID applicationId;

    /** Unique identifier of the Approver rendering the decision. */
    private UUID approverId;

    /**
     * The verdict to apply to the application.
     * Valid values: {@code "APPROVED"} or {@code "REJECTED"}.
     */
    private String decision;

    /**
     * Mandatory justification text explaining the rationale for the decision.
     * Stored alongside the decision for transparency and audit purposes.
     */
    private String notes;
}