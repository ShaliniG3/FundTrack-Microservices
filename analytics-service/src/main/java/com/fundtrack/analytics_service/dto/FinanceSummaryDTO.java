package com.fundtrack.analytics_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object providing a high-level financial overview of a specific program.
 * <p>
 * This DTO aggregates budget allocations, actual spending (disbursements),
 * committed funds (scheduled payments), and overall utilization metrics.
 * </p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Financial summary and budget utilization metrics for a program")
public class FinanceSummaryDTO {

    /**
     * Unique identifier of the program being analyzed.
     */
    @Schema(description = "Unique ID of the program", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID programId;

    /**
     * The total allocated budget for the program.
     */
    @Schema(description = "Total allocated budget", example = "500000.00")
    private Double totalBudget;

    /**
     * Total amount of funds already paid out (status 'PAID').
     */
    @Schema(description = "Total funds actually disbursed to date", example = "120000.50")
    private Double totalFundsDisbursed;

    /**
     * The remaining budget calculated as (Total Budget - Total Disbursed).
     */
    @Schema(description = "Remaining available budget", example = "379999.50")
    private Double fundsRemaining;

    /**
     * Total amount of funds promised or scheduled for future payment (status 'SCHEDULED').
     */
    @Schema(description = "Funds committed but not yet disbursed", example = "50000.00")
    private Double fundsCommitted;

    /**
     * The count of applications that have reached an approved state.
     */
    @Schema(description = "Number of grants approved under this program", example = "24")
    private Long approvedGrants;

    /**
     * Percentage of the total budget that has been disbursed.
     * Calculated as (Total Funds Disbursed / Total Budget) * 100.
     */
    @Schema(description = "Budget utilization percentage", example = "24.0")
    private Double utilisationPercentage;

}