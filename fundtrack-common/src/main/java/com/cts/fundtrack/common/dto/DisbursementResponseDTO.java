package com.cts.fundtrack.common.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.DisbursementStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a scheduled or completed grant disbursement.
 * <p>
 * This DTO provides the specific details for an individual payment installment,
 * including the payout amount, the scheduled date, and the current processing state.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing details of a specific fund disbursement installment")
public class DisbursementResponseDTO {

    /**
     * Unique identifier for the disbursement record.
     */
    @Schema(description = "The unique primary key (UUID) of the disbursement",
            example = "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7")
    private UUID id;

    /**
     * Unique identifier for the associated grant application.
     */
    @Schema(description = "The ID of the application this payment belongs to",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationId;

    /**
     * The monetary value to be disbursed in this installment.
     */
    @Schema(description = "The amount of money for this specific installment", example = "2500.00")
    private Double amount;

    /**
     * The date on which the payment is scheduled to be processed.
     */
    @Schema(description = "The scheduled payout date", example = "2026-04-15")
    private LocalDate scheduledDate;

    /**
     * The current lifecycle status of this specific disbursement.
     * <p>Expected values: PENDING, PAID, CANCELLED, FAILED.</p>
     */
    @Schema(description = "The current status of the payment installment", example = "PENDING")
    private DisbursementStatus status;
}