package com.fundtrack.modules.application_service.dto;

import java.util.UUID;

import com.fundtrack.modules.application_service.models.enums.VerificationStatus;

import lombok.Data;

@Data
public class DocumentDTO {

    private UUID documentId;
        private String docType;
        private String fileUri;
        private VerificationStatus verificationStatus;
    
}
