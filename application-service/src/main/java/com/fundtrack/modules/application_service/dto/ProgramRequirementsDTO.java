package com.fundtrack.modules.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgramRequirementsDTO {
    
    private UUID programId;
    private String programName;
    
    // Combined list of what is needed
    private List<String> requiredDocumentTypes; // ["ID_PROOF", "TAX_RETURN"]
    private List<String> eligibilityRules;      // ["Age > 18", "Resident of NY"]
    
    public List<String> getRequiredDocuments() {
        return this.requiredDocumentTypes;
    }
    // private String deadlineDate;
}
