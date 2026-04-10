package com.cts.fundtrack.dgcs.dto.paymentdto;

import com.cts.fundtrack.dgcs.model.enums.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.util.UUID;

/**
 * Data Transfer Object for initiating a fund transfer.
 * <p>
 * This DTO is used by the Finance/Disbursement module to execute a payment
 * for an approved disbursement installment. It bridges the gap between
 * the 'Scheduled Disbursement' and the actual 'Payment Gateway' transaction,
 * ensuring that the chosen financial channel is recorded for audit purposes.
 * </p>
 */

@Data
@Schema(description = "Request model to execute a payment for a specific disbursement installment")
public class PaymentRequestDTO {

    @NotNull(message = "Disbursement ID is required to process payment.")
    @Schema(description = "The unique identifier of the disbursement installment to be paid",
            example = "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7")
    private UUID disbursementId;

    @NotNull(message = "Payment method is required.")
    @Schema(description = "The specific financial channel selected for this transaction",
            example = "BANK_TRANSFER")
    private PaymentMethod method;
}