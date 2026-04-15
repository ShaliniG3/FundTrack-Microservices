package com.cts.fundtrack.common.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredDocumentDTO {

    /**
     * The unique identifier for the document.
     * Include this to allow partial updates/edits to existing documents.
     */
    private UUID documentId;

    @NotBlank(message = "Document name is required.")
    private String name;

    @NotNull(message = "Mandatory status must be specified.")
    private Boolean mandatory;
}

