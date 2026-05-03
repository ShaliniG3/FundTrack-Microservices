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

/**
 * Service interface defining the core business operations for managing grant
 * applications in the FundTrack system.
 *
 * <p>Implementations orchestrate the full application lifecycle: from initial
 * submission (with document validation and automated eligibility rule evaluation)
 * through updates, data retrieval, and withdrawal. Inter-service communication
 * with the Program Service (for eligibility rules and requirements) and the
 * Notification Service (for applicant alerts) is also coordinated here.</p>
 *
 * @see ApplicationServiceImpl
 */
public interface ApplicationService {

    /**
     * Submits a new grant application for the specified applicant to the program
     * identified in the request DTO.
     *
     * <p>Validates that no duplicate application exists for the applicant/program
     * pair, persists supporting documents, triggers automated eligibility
     * validation, and dispatches a submission confirmation notification.</p>
     *
     * @param applicantId the UUID of the authenticated applicant submitting the application
     * @param dto         the application payload including program ID, freeform data,
     *                    and optional documents
     * @return the persisted {@link ApplicationResponseDTO} with generated ID and
     *         initial status
     * @throws com.cts.fundtrack.common.exceptions.DuplicateApplicationException if the
     *         applicant has already submitted an application to the same program
     */
    ApplicationResponseDTO applyToProgram(UUID applicantId, ApplicationRequestDTO dto);

    /**
     * Updates the data and/or documents of an existing grant application.
     *
     * <p>Updates are only permitted for applications not yet in a terminal state
     * ({@code APPROVED} or {@code REJECTED}). Changing {@code applicationData}
     * resets the status to {@code SUBMITTED} and re-triggers eligibility validation.</p>
     *
     * @param applicationId the UUID of the application to update
     * @param dto           the update payload with new application data and/or documents
     * @return the updated {@link ApplicationResponseDTO}
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     * @throws com.cts.fundtrack.common.exceptions.InvalidApplicationStateException if the
     *         application is in a terminal state that disallows updates
     */
    ApplicationResponseDTO updateApplication(UUID applicationId, ApplicationUpdateDTO dto);

    /**
     * Retrieves the full detail view of a grant application, including associated
     * documents and validation results, for dashboard display.
     *
     * @param applicationId the UUID of the application to retrieve
     * @return the {@link ApplicantDetailsDTO} containing all application fields
     *         and related collections
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    ApplicantDetailsDTO getFullApplicationDetails(UUID applicationId);

    /**
     * Retrieves the automated eligibility rule evaluation results for an application.
     *
     * @param applicationId the UUID of the application whose validation results are needed
     * @return a list of {@link ValidationResultDTO} objects, one per evaluated rule;
     *         empty if no rules have been evaluated yet
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    List<ValidationResultDTO> getValidationResults(UUID applicationId);

    /**
     * Retrieves all documents attached to a specific grant application.
     *
     * @param applicationId the UUID of the application whose documents are requested
     * @return a list of {@link DocumentDTO} objects; empty if no documents have
     *         been uploaded
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    List<DocumentDTO> getDocumentsByApplicationId(UUID applicationId);

    /**
     * Retrieves the program requirements checklist applicable to a specific application
     * by delegating to the Program Service via Feign.
     *
     * @param applicationId the UUID of the application whose program requirements
     *                      are requested
     * @return the {@link ProgramRequirementsDTO} for the application's target program,
     *         or {@code null} if the Program Service is unavailable
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    ProgramRequirementsDTO getRequirementsByApplication(UUID applicationId);

    /**
     * Executes automated eligibility validation for the specified application.
     * Called internally (via the Spring proxy) after submission or data update
     * so that {@code @Transactional} and {@code @Auditable} are applied correctly.
     *
     * @param applicationId the UUID of the application to validate
     */
    void performValidation(UUID applicationId);

    /**
     * Permanently deletes a grant application and all its associated documents
     * and validation records.
     *
     * @param applicationId the UUID of the application to delete
     * @throws com.cts.fundtrack.common.exceptions.ApplicationNotFoundException if no
     *         application exists with the given ID
     */
    void deleteApplication(UUID applicationId);

    /**
     * Retrieves all grant applications submitted by a specific applicant.
     *
     * @param applicantId the UUID of the applicant whose applications are requested
     * @return a list of {@link ApplicationResponseDTO} objects; empty if the
     *         applicant has no applications on record
     */
    List<ApplicationResponseDTO> getMyApplications(UUID applicantId);

    /**
     * Retrieves all grant applications submitted to a specific program.
     *
     * <p>Used by the Finance/Disbursement dashboard to list APPROVED and ACCEPTED
     * applicants for payment scheduling, and by the Analytics Service for status
     * distribution and financial summary calculations.</p>
     *
     * @param programId the UUID of the grant program
     * @return a list of {@link ApplicationResponseDTO} objects for all applications
     *         in the program; empty if no applications exist for the program
     */
    List<ApplicationResponseDTO> getApplicationsByProgramId(UUID programId);

    /**
     * Returns the UUIDs of all APPROVED applications for a program.
     * Called internally by the Disbursement Service to determine which
     * applicants receive a budget share during finalization.
     *
     * @param programId the UUID of the program
     * @return list of approved application UUIDs; empty if none
     */
    List<UUID> getApprovedApplicationIds(UUID programId);

    /**
     * Returns {@code true} if any application for the program is still in
     * {@code SUBMITTED} or {@code UNDER_REVIEW} status, blocking premature
     * budget finalization.
     *
     * @param programId the UUID of the program
     * @return {@code true} if pending reviews exist; {@code false} otherwise
     */
    Boolean hasPendingReviews(UUID programId);

    /**
     * Updates the lifecycle status of an application.
     * Called internally by the Disbursement Service to move an application
     * to {@code ACCEPTED} after its installment schedule is created.
     *
     * @param applicationId the UUID of the application to update
     * @param newStatus     the target status string (e.g., {@code "ACCEPTED"})
     */
    void updateApplicationStatus(UUID applicationId, String newStatus);

    /**
     * Returns lightweight metadata for a single application.
     * Used by the Disbursement Service for display and audit purposes.
     *
     * @param applicationId the UUID of the application
     * @return an {@link com.cts.fundtrack.common.dto.ApplicationMetadataDTO}
     *         with applicant ID, status, and best-effort name fields
     */
    com.cts.fundtrack.common.dto.ApplicationMetadataDTO getApplicationMetadata(UUID applicationId);
}