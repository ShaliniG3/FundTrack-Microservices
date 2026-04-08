package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Simplified status tracking for the disbursement lifecycle.
 */
@Schema(description = "Lifecycle states for individual financial installments or payout batches")
public enum DisbursementStatus {

    @Schema(description = "The entire disbursement process for this installment is finalized and verified")
    COMPLETED,

    @Schema(description = "Funds have been successfully transferred to the recipient's account")
    PAID,

    @Schema(description = "The payout date is set, but the transaction has not yet been initiated")
    SCHEDULED,

    PENDING, @Schema(description = "The disbursement was halted (e.g., due to compliance failure or program closure)")
    CANCELLED
}