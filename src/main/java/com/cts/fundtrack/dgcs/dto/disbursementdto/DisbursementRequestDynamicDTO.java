package com.cts.fundtrack.dgcs.dto.disbursementdto;

import com.cts.fundtrack.dgcs.model.enums.PaymentFrequency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for triggering dynamic disbursement scheduling at the program level.
 * <p>
 * This DTO is used to define the payout structure (frequency and count) for all
 * approved applications within a specific funding program. It serves as the
 * configuration for the automated payment engine.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for configuring dynamic payment frequencies for a program")
public class DisbursementRequestDynamicDTO {

    /**
     * Unique identifier for the funding program.
     */
    @NotNull(message = "Program ID is required to finalize awards.")
    @Schema(description = "The unique ID of the program to schedule disbursements for",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID programId;

    /**
     * The interval at which payments should be disbursed.
     * <p>Supported values: MONTHLY, QUARTERLY, BI_ANNUALLY, ANNUALLY.</p>
     */
    @NotNull(message = "Payout frequency must be specified (MONTHLY, QUARTERLY, etc.).")
    @Schema(description = "The recurring interval for payments", example = "QUARTERLY")
    private PaymentFrequency frequency;

    /**
     * The total number of payment cycles to be generated.
     * <p>Example: A 2-year grant with QUARTERLY frequency would require 8 payments.</p>
     */
    @NotNull(message = "Frequency of the payment must be specified.")
    @Schema(description = "Total number of installments to be scheduled", example = "4")
    private int numberOfPayments;

}