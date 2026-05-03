package com.cts.fundtrack.common.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing a document type that applicants must supply
 * when applying for a specific funding program.
 *
 * <p>Defined by program administrators as part of a {@link ProgramRequestDTO} or
 * {@link ProgramResponseDTO}. During application validation the Application Service
 * checks that every {@code mandatory} document type has a corresponding uploaded
 * {@link DocumentDTO}.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredDocumentDTO {

    /**
     * Unique identifier of an existing required-document record.
     * Include this when updating an existing entry; omit it to create a new one.
     */
    private UUID documentId;

    /**
     * Human-readable name / type key of the document (e.g., {@code "ID_PROOF"},
     * {@code "INCOME_CERTIFICATE"}). Must not be blank.
     */
    @NotBlank(message = "Document name is required.")
    private String name;

    /**
     * Indicates whether this document type is compulsory for a complete application.
     * If {@code true}, the absence of this document will fail the validation check.
     */
    @NotNull(message = "Mandatory status must be specified.")
    private Boolean mandatory;
}

