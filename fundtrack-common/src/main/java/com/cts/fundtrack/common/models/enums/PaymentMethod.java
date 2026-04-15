package com.cts.fundtrack.common.models.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the specific payment channels available for grant disbursements.
 * Limited to the four primary methods used by the Finance Officer.
 */
@Schema(description = "The financial channel or medium used to transfer grant funds to a recipient")
public enum PaymentMethod {

    @Schema(description = "Physical cash disbursement; typically used for small grants or immediate aid")
    CASH,

    @Schema(description = "A physical paper check issued by the organization's bank")
    CHEQUE,

    @Schema(description = "Direct electronic bank transfer (e.g., NEFT, IMPS, or RTGS)")
    BANK_TRANSFER,

    @Schema(description = "Instant digital mobile payment via Unified Payments Interface")
    UPI
}