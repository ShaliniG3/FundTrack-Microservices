package com.fundtrack.analytics_service.dto.programdto;

import com.fundtrack.analytics_service.dto.eligibilitydto.EligibilityRuleDTO;
import com.fundtrack.analytics_service.dto.requireddocdto.RequiredDocumentDTO;
import com.fundtrack.analytics_service.model.external.ProgramStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing the read-only view of a Program.
 * <p>
 * This class is used to serve program details to applicants and administrators.
 * It includes system-managed metadata such as the unique identifier and the
 * current operational status.
 * </p>
 */
@Data
@Schema(description = "Response model for viewing detailed information about a grant program")
public class ProgramResponseDTO {

    /**
     * The unique system-generated identifier (UUID) for the program.
     */
    @Schema(description = "The unique internal ID of the program",
            example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
    private UUID programId;

    /**
     * The name of the program.
     */
    @Schema(description = "The title of the funding program", example = "Urban Innovation Fund")
    private String name;

    /**
     * A detailed description of the program.
     */
    @Schema(description = "The narrative description of program goals")
    private String description;

    /**
     * The allocated budget for the program.
     */
    @Schema(description = "Total financial allocation for the program", example = "500000.00")
    private Double budget;

    /**
     * The date the program is scheduled to start.
     */
    @Schema(description = "The date the program opens for applications")
    private LocalDate startDate;

    /**
     * The date the program is scheduled to conclude.
     */
    @Schema(description = "The final deadline for application submission")
    private LocalDate endDate;

    /**
     * The current operational state of the program.
     * <p>Expected values: DRAFT, ACTIVE, CLOSED, ARCHIVED.</p>
     */
    @Schema(description = "The current lifecycle status of the program", example = "ACTIVE")
    private ProgramStatus status;

    /**
     * A list of eligibility criteria associated with this program.
     */
    @Schema(description = "Array of rules that define applicant eligibility")
    private List<EligibilityRuleDTO> eligibilityRules;

    /**
     * A list of documents necessary for application.
     */
    @Schema(description = "Array of mandatory document types required for submission")
    private List<RequiredDocumentDTO> requiredDocuments;
}