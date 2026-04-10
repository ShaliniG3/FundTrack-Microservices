package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration representing the real-time transaction state of a specific fund transfer.
 * <p>
 * This status tracks the technical success or failure of a payment record. While
 * DisbursementStatus handles the broad business state, PaymentStatus provides
 * the audit trail for bank-level reconciliation, ensuring every penny is
 * accounted for in the fiduciary oversight process.
 * </p>
 */
@Schema(description = "The current transactional status of a disbursement payment")
public enum PaymentStatus {

    @Schema(description = "The transaction has been initiated or scheduled but not yet confirmed by the financial institution.")
    PENDING,

    @Schema(description = "The funds have been successfully cleared and reached the recipient's account.")
    SUCCESS,

    @Schema(description = "The transaction was rejected or failed. This may trigger a 'Flagged for Review' state in compliance.")
    FAILED,

    @Schema(description = "The payment was successfully processed but subsequently reversed or returned to the source account.")
    REFUNDED
}