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
 * configuration for the automated payment engine to generate milestone-based
 * schedules and set the reporting cadence.
 * </p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for configuring dynamic payment frequencies for a program")
public class DisbursementRequestDynamicDTO {

    @NotNull(message = "Program ID is required to finalize awards.")
    @Schema(description = "The unique identifier of the program to schedule disbursements for",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID programId;

    @NotNull(message = "Payout frequency must be specified.")
    @Schema(description = "The recurring interval for payments (e.g., MONTHLY, QUARTERLY)",
            example = "QUARTERLY")
    private PaymentFrequency frequency;

    @NotNull(message = "Total number of installments must be specified.")
    @Schema(description = "The total number of payment cycles to be generated for each application",
            example = "4")
    private Integer numberOfPayments;
}