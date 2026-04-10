package com.cts.fundtrack.dgcs.dto.disbursementdto;

import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing a scheduled or completed grant disbursement.
 * <p>
 * This DTO provides the specific details for an individual payment installment,
 * acting as the primary data model for the Finance Dashboard and Applicant
 * payout history views. It encapsulates the financial commitment and its
 * current realization state.
 * </p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing details of a specific fund disbursement installment")
public class DisbursementResponseDTO {

    @Schema(description = "The unique primary key (UUID) of the disbursement",
            example = "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7")
    private UUID id;

    @Schema(description = "The ID of the application this payment belongs to",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID applicationId;

    @Schema(description = "The monetary value to be disbursed in this installment",
            example = "2500.00")
    private Double amount;

    @Schema(description = "The scheduled payout date for this installment",
            example = "2026-04-15")
    private LocalDate scheduledDate;

    @Schema(description = "The current lifecycle status of the payment installment",
            example = "PENDING")
    private DisbursementStatus status;
}