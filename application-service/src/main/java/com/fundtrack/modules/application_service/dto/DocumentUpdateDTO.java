package com.fundtrack.modules.application_service.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateDTO {
    private UUID documentId; // Optional: include if updating an existing file
        private String docType;
        private String fileUri;
}
