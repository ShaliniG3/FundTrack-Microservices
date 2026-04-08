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
 * fulfilling security requirements for financial data handling.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model providing a secure receipt of a completed or pending payment")
public class PaymentResponseDTO {

    /**
     * The encrypted version of the internal Payment ID.
     * Use this string for any client-side routing or public URL parameters.
     */
    @Schema(description = "Encrypted payment reference for secure client-side usage",
            example = "U2FsdGVkX19v8nZ...")
    private String encryptedPaymentId;

    /**
     * The total monetary value disbursed in this transaction.
     */
    @Schema(description = "The total amount paid", example = "2500.50")
    private Double amount;

    /**
     * The precise timestamp when the payment was processed.
     */
    @Schema(description = "Timestamp of the transaction execution")
    private Instant date;

    /**
     * The financial channel used for the transfer.
     */
    @Schema(description = "The method used for payment", example = "UPI")
    private PaymentMethod method;

    /**
     * The current lifecycle status of the transaction.
     * <p>Expected values: PENDING, COMPLETED, FAILED, REVERSED.</p>
     */
    @Schema(description = "The current status of the payment", example = "COMPLETED")
    private PaymentStatus status;

    /**
     * The unique identifier of the parent disbursement installment.
     */
    @Schema(description = "The internal ID of the linked disbursement",
            example = "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7")
    private UUID disbursementId;
}