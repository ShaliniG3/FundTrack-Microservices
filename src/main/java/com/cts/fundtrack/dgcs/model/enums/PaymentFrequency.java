package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the recurring interval at which grant disbursements are scheduled.
 */
@Schema(description = "The recurring frequency for fund disbursement installments")
public enum PaymentFrequency {

    @Schema(description = "Installments are paid out once every month")
    MONTHLY,

    @Schema(description = "Installments are paid out every three months (4 times a year)")
    QUARTERLY,

    @Schema(description = "Installments are paid out every six months (2 times a year)")
    HALF_YEARLY,

    @Schema(description = "The total grant amount is paid out in a single annual installment")
    YEARLY
}