package com.cts.fundtrack.common.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight Data Transfer Object carrying the essential metadata for a funding program.
 *
 * <p>Used in contexts where the full {@link ProgramResponseDTO} payload is not needed —
 * for example, when the Finance Service needs to check whether a program is still open
 * before scheduling disbursements, or when computing each applicant's individual grant
 * share from the total budget.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramMetadataDTO {

    /** Unique identifier of the funding program. */
    private UUID programId;

    /** Human-readable name of the program. */
    private String name;

    /**
     * Current lifecycle status of the program (e.g., {@code "ACTIVE"}, {@code "CLOSED"}).
     * Used to gate operations that must not proceed after program closure.
     */
    private String status;

    /**
     * Total allocated budget for the program in the system's base currency.
     * Used to calculate each approved applicant's proportional grant share.
     */
    private Double budget;
}