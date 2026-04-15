package com.cts.fundtrack.application.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.ApplicantDetailsDTO;
import com.cts.fundtrack.common.dto.ApplicationRequestDTO;
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.ApplicationUpdateDTO;
import com.cts.fundtrack.common.dto.DocumentDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;
import com.cts.fundtrack.common.dto.ValidationResultDTO;

public interface ApplicationService {
    ApplicationResponseDTO applyToProgram(UUID applicantId, ApplicationRequestDTO dto);
    ApplicationResponseDTO updateApplication(UUID applicationId, ApplicationUpdateDTO dto);
    ApplicantDetailsDTO getFullApplicationDetails(UUID applicationId);
    List<ValidationResultDTO> getValidationResults(UUID applicationId);
    List<DocumentDTO> getDocumentsByApplicationId(UUID applicationId);
    ProgramRequirementsDTO getRequirementsByApplication(UUID applicationId);
    void deleteApplication(UUID applicationId);
}