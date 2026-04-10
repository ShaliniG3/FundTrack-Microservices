package com.cts.fundtrack.dgcs.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration defining the temporal intervals for recurring grant disbursements.
 * <p>
 * This frequency is established during the award finalization phase and dictates
 * the automated generation of disbursement schedules. It ensures that both
 * the Finance Officer and the Applicant have clear expectations regarding the
 * timing of fund releases and the corresponding reporting deadlines.
 * </p>
 */
@Schema(description = "The recurring frequency for fund disbursement installments")
public enum PaymentFrequency {

    @Schema(description = "Installments are scheduled and paid out on a monthly basis.")
    MONTHLY,

    @Schema(description = "Installments are scheduled and paid out every three months (4 times per year).")
    QUARTERLY,

    @Schema(description = "Installments are scheduled and paid out every six months (2 times per year).")
    HALF_YEARLY,

    @Schema(description = "The total grant amount is released in a single installment once per year.")
    YEARLY
}