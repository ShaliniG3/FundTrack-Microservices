package com.fundtrack.modules.application_service.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationRequestDTO {

    @NotNull(message = "Program ID is required")
    private UUID programId;

    @NotNull(message = "Applicant ID is required")
    private UUID applicantId;

    private String applicationData;

    /**
     * Requirement 4.3: Capture supporting documents in the same request.
     * Key: Document Type (e.g., "PAN_CARD", "INCOME_CERT")
     * Value: The URL or Path where the file is stored
     */
    private Map<String, String> documents;
    
}
