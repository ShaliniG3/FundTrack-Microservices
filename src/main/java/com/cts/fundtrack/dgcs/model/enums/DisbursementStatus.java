package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration representing the lifecycle states of a grant disbursement installment.
 * <p>
 * This status governs the financial workflow, moving from initial scheduling
 * to final reconciliation. It acts as a trigger for the Grant Reporting module,
 * as disbursements marked as PAID or COMPLETED typically require an associated
 * progress report from the applicant.
 * </p>
 */
@Schema(description = "Lifecycle states for individual financial installments or payout batches")
public enum DisbursementStatus {

    @Schema(description = "The payout date is set, but the transaction has not yet been initiated.")
    SCHEDULED,

    @Schema(description = "The disbursement is awaiting internal approval or liquidity verification.")
    PENDING,

    @Schema(description = "Funds have been successfully transferred to the recipient's account.")
    PAID,

    @Schema(description = "The entire disbursement process, including reconciliation and audit, is finalized.")
    COMPLETED,

    @Schema(description = "The disbursement was halted (e.g., due to compliance failure or program closure).")
    CANCELLED
}