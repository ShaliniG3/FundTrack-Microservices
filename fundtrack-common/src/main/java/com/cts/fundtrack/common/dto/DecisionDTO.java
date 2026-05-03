package com.cts.fundtrack.common.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing a formal approval decision made on a grant application.
 *
 * <p>Created by the Decision Service when an Approver finalises their verdict on an
 * application that has passed the review phase. The decision record is immutable once
 * created and serves as the authoritative record for downstream disbursement scheduling.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionDTO {

    /** Unique identifier of this decision record. */
    private UUID decisionId;

    /** Unique identifier of the application this decision applies to. */
    private UUID applicationId;

    /** Unique identifier of the Approver who rendered this decision. */
    private UUID approverId;

    /**
     * The verdict rendered by the Approver.
     * Valid values: {@code "APPROVED"} or {@code "REJECTED"}.
     */
    private String decision;

    /**
     * Free-text justification provided by the Approver explaining the rationale
     * behind the decision.
     */
    private String notes;

    /** The calendar date on which the decision was finalised. */
    private LocalDate date;
}