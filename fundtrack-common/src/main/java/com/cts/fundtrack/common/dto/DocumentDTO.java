package com.cts.fundtrack.common.dto;

import java.util.UUID;

import com.cts.fundtrack.common.models.enums.VerificationStatus;

import lombok.Data;

@Data
public class DocumentDTO {

    private UUID documentId;
        private String docType;
        private String fileUri;
        private VerificationStatus verificationStatus;
    
}