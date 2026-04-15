package com.cts.fundtrack.common.dto;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.ApplicationStatus;

import lombok.Data;

@Data
public class ApplicantDetailsDTO {
    // Core Application Info
    private UUID applicationId;
    private UUID programId;
    private ApplicationStatus status;
    private String applicationData;
    private LocalDateTime createdAt;

    // Associated Documents
    private List<DocumentDTO> documents;

    // Associated Validation Results
    private List<ValidationResultDTO> validationResults;
}