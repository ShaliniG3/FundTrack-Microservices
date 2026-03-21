package com.fundtrack.analytics_service.dto.applicationdto;

import com.fundtrack.analytics_service.dto.documentdto.DocumentDTO;
import com.fundtrack.analytics_service.dto.documentdto.ValidationDTO;
import com.fundtrack.analytics_service.model.external.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a comprehensive view of a fund application.
 * <p>
 * This DTO is used to return detailed application data to the client, including
 * associated program information, user details, and the results of any
 * document validations.
 * </p>
 */
@Data
@Builder
@Schema(description = "Detailed response object containing full application state and associated metadata")
public class ApplicationResponseDTO {

    /**
     * The unique identifier of the application record.
     */
    @Schema(description = "Unique ID of the application", example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
    private UUID applicationId;

    /**
     * Unique identifier for the funding program.
     */
    @Schema(description = "ID of the associated funding program", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID programId;

    /**
     * Unique identifier for the applicant user.
     */
    @Schema(description = "ID of the user who submitted the application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID userId;

    /**
     * The full name of the applicant for display purposes.
     */
    @Schema(description = "Display name of the applicant", example = "John Doe")
    private String userName;

    /**
     * The name of the funding program.
     */
    @Schema(description = "Name of the funding program", example = "Education Grant 2026")
    private String programName;

    /**
     * The current processing status of the application.
     */
    @Schema(description = "Current lifecycle status of the application", example = "UNDER_REVIEW")
    private ApplicationStatus status;

    /**
     * The timestamp when the application was initially submitted.
     */
    @Schema(description = "Timestamp of initial submission")
    private Instant submittedDate;

    /**
     * List of supporting documents attached to this application.
     */
    @Schema(description = "Collection of uploaded documents and their metadata")
    private List<DocumentDTO> documents;

    /**
     * List of validation results (e.g., OCR or manual checks) performed on the documents.
     */
    @Schema(description = "Collection of validation checks performed on application documents")
    private List<ValidationDTO> validations;
}