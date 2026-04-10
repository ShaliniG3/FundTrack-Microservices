package com.cts.fundtrack.dgcs.dto.paymentdto;

import com.cts.fundtrack.dgcs.model.enums.PaymentMethod;
import com.cts.fundtrack.dgcs.model.enums.PaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing the final receipt of a processed payment.
 * <p>
 * This DTO provides a secure view of a financial transaction. It utilizes
 * ID encryption to shield internal database primary keys from public exposure,
 * ensuring that sensitive financial records are protected during client-side
 * transmission and audit-trail rendering.
 * </p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model providing a secure receipt of a completed or pending payment")
public class PaymentResponseDTO {

    @Schema(description = "Encrypted payment reference for secure client-side usage and public URL masking",
            example = "U2FsdGVkX19v8nZ_secure_token_xyz")
    private String encryptedPaymentId;

    @Schema(description = "The total monetary value successfully disbursed in this transaction",
            example = "2500.50")
    private Double amount;

    @Schema(description = "The precise ISO-8601 timestamp when the payment was processed by the system",
            example = "2026-04-09T10:00:00Z")
    private Instant date;

    @Schema(description = "The financial channel used for the transfer (e.g., BANK_TRANSFER, UPI)",
            example = "UPI")
    private PaymentMethod method;

    @Schema(description = "The current real-time status of the transaction from the payment gateway",
            example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "The internal identifier of the linked disbursement installment",
            example = "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7")
    private UUID disbursementId;
}