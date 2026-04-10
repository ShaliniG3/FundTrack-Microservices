package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration defining the technical mediums for fund transfer within the disbursement lifecycle.
 * <p>
 * This classification allows the Finance Module to categorize transactions for bank reconciliation
 * and audit purposes. It ensures that the payout method selected by the Finance Officer aligns
 * with the recipient's registered financial details.
 * </p>
 */
@Schema(description = "The financial channel or medium used to transfer grant funds to a recipient")
public enum PaymentMethod {

    @Schema(description = "Physical currency disbursement; typically restricted to emergency aid or micro-grants.")
    CASH,

    @Schema(description = "A physical negotiable instrument issued via the organization's corporate bank account.")
    CHEQUE,

    @Schema(description = "Electronic Fund Transfer (EFT) including Wire, NEFT, IMPS, or RTGS protocols.")
    BANK_TRANSFER,

    @Schema(description = "Instant mobile-based digital payment via the Unified Payments Interface.")
    UPI
}