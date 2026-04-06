package com.fundtrack.modules.application_service.service;

import java.util.List;
import java.util.UUID;
import com.fundtrack.modules.application_service.dto.*;

/**
 * Service interface for Grant Application Management.
 * Handles the unified intake, validation, and status tracking.
 */
public interface ApplicationService {

    /**
     * Requirement 2.3 & 4.3: Unified submission of data and documents.
     * @param applicantId The ID of the logged-in user (extracted from Security Context)
     * @param dto Combined data and document file URLs
     */
    ApplicationResponseDTO applyToProgram(UUID applicantId, ApplicationRequestDTO dto);

    /**
     * Requirement 1.2: Update application data before final approval.
     */
    ApplicationResponseDTO updateApplication(UUID applicationId, ApplicationUpdateDTO dto);

    /**
     * Requirement 7.1: Fetch full details for the Applicant Dashboard.
     */
    ApplicantDetailsDTO getFullApplicationDetails(UUID applicationId);

    /**
     * Requirement 4.3: Fetch automated validation results.
     */
    List<ValidationResultDTO> getValidationResults(UUID applicationId);

    /**
     * Requirement 4.5: Fetch documents for the Decision/Approval module.
     */
    List<DocumentDTO> getDocumentsByApplicationId(UUID applicationId);

    /**
     * Requirement 2.2: Fetch rules and required docs from Program Service.
     */
    ProgramRequirementsDTO getRequirementsByApplication(UUID applicationId);

    /**
     * Withdraw/Remove an application.
     */
    void deleteApplication(UUID applicationId);
    
}