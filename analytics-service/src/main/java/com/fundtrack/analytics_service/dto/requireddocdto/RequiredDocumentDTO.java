package com.fundtrack.analytics_service.dto.requireddocdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object representing a document requirement for a grant program.
 * <p>
 * This DTO is used to define the types of files (e.g., "Tax Returns", "Identity Proof")
 * that an applicant needs to provide. It supports both the definition phase
 * (Program Creation) and the submission phase (Application Checklist).
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Model representing a mandatory or optional document requirement")
public class RequiredDocumentDTO {

    /**
     * The unique identifier for the document requirement.
     * <p>Optional for creation, mandatory for updates to existing requirements.</p>
     */
    @Schema(description = "Unique ID of the document requirement",
            example = "c1d2e3f4-g5h6-7i8j-9k0l-m1n2o3p4q5r6")
    private UUID documentId;

    /**
     * The display name or title of the document.
     */
    @NotBlank(message = "Document name is required.")
    @Schema(description = "The title of the required document", example = "Audited Financial Statement")
    private String name;

    /**
     * Flag indicating if the document is strictly required for application submission.
     */
    @NotNull(message = "Mandatory status must be specified.")
    @Schema(description = "Flag indicating if the document is compulsory", example = "true")
    private Boolean mandatory;
}