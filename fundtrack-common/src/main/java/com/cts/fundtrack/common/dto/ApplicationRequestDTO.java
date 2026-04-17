package com.cts.fundtrack.common.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * Data Transfer Object for creating or updating a grant application.
 * <p>
 * This DTO encapsulates the necessary identifiers for the program and user,
 * along with the current lifecycle status and any associated supporting documentation.
 * </p>
 */
@Data
@Schema(description = "Request object for submitting or updating a fund application")
public class ApplicationRequestDTO {

    /**
     * Unique identifier for the specific funding program.
     */
    @Schema(description = "The unique ID of the funding program", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID programId;

    /**
     * Unique identifier for the user (Applicant) submitting the request.
     */
    @Schema(description = "The unique ID of the user applying for the fund", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID userId;


    /**
     * Free-text narrative the applicant provides describing their project and eligibility.
     * This field is evaluated against SpEL eligibility rules during validation.
     */
    @Schema(description = "Applicant's data / project description used for eligibility evaluation")
    private String applicationData;

    /**
     * A collection of documents attached to the application for verification.
     */
    @Schema(description = "List of supporting documents uploaded with the application")
    private List<DocumentDTO> documents;
}