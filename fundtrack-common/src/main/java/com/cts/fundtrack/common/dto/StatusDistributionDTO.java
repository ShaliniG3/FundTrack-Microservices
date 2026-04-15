package com.cts.fundtrack.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing the full status breakdown for a specific program.
 * <p>
 * This DTO aggregates individual {@link StatusCountDTO} objects into a collection,
 * providing a complete overview of application volumes across all lifecycle stages.
 * It is typically used to populate pie charts or distribution graphs.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Wrapper object containing the list of status counts for a specific program ID")
public class StatusDistributionDTO {

   /**
    * Unique identifier of the program for which the distribution is calculated.
    */
   @Schema(description = "The unique ID of the program", example = "550e8400-e29b-41d4-a716-446655440000")
   private UUID programId;

   /**
    * A list of statuses and their corresponding application counts.
    */
   @Schema(description = "List of counts categorized by application status")
   private List<StatusCountDTO> statusDistribution;

}