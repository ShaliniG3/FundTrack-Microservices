package com.fundtrack.modules.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ApplicationUpdateDTO {

    private String applicationData;

    // Optional: List of documents to update or add
    // If this is null or empty, the service will ignore it
    private List<DocumentDTO> documents;
    
}
