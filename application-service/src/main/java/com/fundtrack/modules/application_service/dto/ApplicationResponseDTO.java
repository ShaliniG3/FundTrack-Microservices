package com.fundtrack.modules.application_service.dto;

import java.time.LocalDateTime;
 // Make sure to import List
import java.util.UUID;

import lombok.Data;

@Data
public class ApplicationResponseDTO {

    private UUID applicationId;
    private UUID programId;
    private UUID applicantId;
    private String status;
    private LocalDateTime submittedDate;
    
    // Add these two fields so the Mapper can set the lists
    
}