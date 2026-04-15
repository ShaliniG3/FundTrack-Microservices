package com.cts.fundtrack.common.dto;

import java.util.UUID;

import com.cts.fundtrack.common.models.enums.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Data Transfer Object for initiating a fund transfer.
 * <p>
 * This DTO is used by the Finance/Disbursement module to execute a payment
 * for an approved disbursement installment. It bridges the gap between
 * the 'Scheduled Disbursement' and the actual 'Payment Gateway' transaction.
 * </p>
 */
@Data
@Schema(description = "Request model to execute a payment for a specific disbursement installment")
public class PaymentRequestDTO {

    /**
     * The unique identifier of the approved disbursement record.
     */
    @NotNull(message = "Disbursement ID is required to process payment.")
    @Schema(description = "The ID of the disbursement installment to be paid",
            example = "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7")
    private UUID disbursementId;

    /**
     * The chosen financial channel for the transaction.
     * <p>Expected values: BANK_TRANSFER, UPI, CHECK, NEFT.</p>
     */
    @NotNull(message = "Payment method is required.")
    @Schema(description = "The financial method used for the transfer", example = "BANK_TRANSFER")
    private PaymentMethod method;
}