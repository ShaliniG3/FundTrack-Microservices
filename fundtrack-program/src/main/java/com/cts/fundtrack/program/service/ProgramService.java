package com.cts.fundtrack.program.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequestDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.models.enums.ProgramStatus;

/**
 * Service interface defining the business operations for grant funding program management.
 *
 * <p>This interface forms the core contract of the Program Microservice's business layer.
 * Implementations are responsible for orchestrating persistence via repositories,
 * enforcing lifecycle rules (e.g., status transition guards), and triggering
 * cross-service communication such as audit logging and user notifications.</p>
 *
 * <p>Role-based visibility logic (e.g., restricting applicants to {@code ACTIVE}/{@code CLOSED}
 * programs) is also enforced within implementations of this interface rather than at
 * the controller layer, keeping security concerns close to the data access logic.</p>
 */
public interface ProgramService {

    /**
     * Creates a new grant funding program from the provided request data.
     *
     * <p>The program is persisted with an initial status of {@code DRAFT}. All nested
     * eligibility rules and required documents supplied in the request are also persisted
     * and linked to the new program in a single transaction. An internal notification
     * is dispatched to the currently authenticated user upon successful creation.</p>
     *
     * @param dto the request data transfer object containing program name, description,
     *            budget, start/end dates, eligibility rules, and required documents.
     * @return a {@link ProgramResponseDTO} representing the newly created program,
     *         including its generated UUID and all nested collections.
     * @throws com.cts.fundtrack.common.exceptions.InvalidProgramStateException if the
     *         provided DTO is null or if the end date precedes the start date.
     */
    ProgramResponseDTO createProgram(ProgramRequestDTO dto);

    /**
     * Retrieves all grant funding programs visible to the currently authenticated user.
     *
     * <p>Applicants (users with role {@code ROLE_APPLICANT}) only receive programs in
     * {@code ACTIVE} or {@code CLOSED} status. Admin and staff users receive all programs
     * regardless of status.</p>
     *
     * @return a list of {@link ProgramResponseDTO} objects. Returns an empty list if no
     *         programs match the caller's visibility constraints.
     */
    List<ProgramResponseDTO> getAllPrograms();

    /**
     * Retrieves a single grant funding program by its unique identifier.
     *
     * <p>Applicants are prevented from viewing programs in statuses other than
     * {@code ACTIVE} or {@code CLOSED}. An {@code AccessDeniedException} is thrown
     * if an applicant attempts to access a restricted program.</p>
     *
     * @param programId the UUID of the program to retrieve.
     * @return the {@link ProgramResponseDTO} for the requested program.
     * @throws com.cts.fundtrack.common.exceptions.ProgramNotFoundException if no program
     *         exists with the given {@code programId}.
     * @throws org.springframework.security.access.AccessDeniedException if an applicant
     *         attempts to view a program that is not {@code ACTIVE} or {@code CLOSED}.
     */
    ProgramResponseDTO getProgramById(UUID programId);

    /**
     * Updates the core details of an existing grant funding program.
     *
     * <p>Only non-null fields present in the request DTO are applied to the existing
     * program entity. The program's status and UUID are not modified by this operation.
     * An audit event is recorded and a notification sent upon success.</p>
     *
     * @param programId the UUID of the program to update.
     * @param dto       the request DTO containing the fields to update. Null fields are ignored.
     * @return the {@link ProgramResponseDTO} reflecting the updated program state.
     * @throws com.cts.fundtrack.common.exceptions.ProgramNotFoundException if no program
     *         exists with the given {@code programId}.
     * @throws com.cts.fundtrack.common.exceptions.InvalidProgramStateException if the
     *         resulting date range is invalid (end date before start date).
     */
    ProgramResponseDTO updateProgram(UUID programId, ProgramRequestDTO dto);

    /**
     * Archives a grant funding program by setting its status to {@code ARCHIVED}.
     *
     * <p>Archiving is a soft deactivation — the program record is retained in the database
     * but is no longer visible to applicants. An audit event is recorded and a notification
     * dispatched upon success.</p>
     *
     * @param programId the UUID of the program to archive.
     * @throws com.cts.fundtrack.common.exceptions.ProgramNotFoundException if no program
     *         exists with the given {@code programId}.
     */
    void archiveProgram(UUID programId);

    /**
     * Permanently deletes a grant funding program and all its cascaded child data.
     *
     * <p>This is an irreversible hard-delete. The program record, all associated eligibility
     * rules, and all required documents are removed from the database. An audit event is
     * recorded and a security confirmation notification is dispatched upon success.</p>
     *
     * @param programId the UUID of the program to permanently delete.
     * @throws com.cts.fundtrack.common.exceptions.ProgramNotFoundException if no program
     *         exists with the given {@code programId}.
     */
    void deleteProgram(UUID programId);

    /**
     * Searches for grant funding programs by a keyword matched against name and description.
     *
     * <p>The search is case-insensitive. Applicants only receive results for programs in
     * {@code ACTIVE} or {@code CLOSED} status. Admin and staff searches span all statuses.</p>
     *
     * @param keyWord the search term to match against the program's name and description fields.
     * @return a list of {@link ProgramResponseDTO} objects matching the keyword and the
     *         caller's visibility constraints. Returns an empty list if no matches are found.
     */
    List<ProgramResponseDTO> searchPrograms(String keyWord);

    /**
     * Transitions a grant funding program to the specified lifecycle status.
     *
     * <p>Activation to {@code ACTIVE} is guarded by a business rule: the program must have
     * at least one eligibility rule and one required document defined. An audit event is
     * recorded and a workflow notification dispatched upon each successful transition.</p>
     *
     * @param programId the UUID of the program whose status is to be changed.
     * @param status    the target {@link ProgramStatus} to transition the program to.
     * @return the {@link ProgramResponseDTO} reflecting the new status.
     * @throws com.cts.fundtrack.common.exceptions.ProgramNotFoundException if no program
     *         exists with the given {@code programId}.
     * @throws com.cts.fundtrack.common.exceptions.InvalidProgramStateException if transitioning
     *         to {@code ACTIVE} and the program lacks eligibility rules or required documents.
     */
    ProgramResponseDTO updateProgramStatus(UUID programId, ProgramStatus status);

    /**
     * Retrieves all eligibility rules associated with a given program.
     *
     * <p>This method is primarily intended for internal use by the Application Service
     * via a Feign client, to evaluate whether an applicant meets the program's criteria.</p>
     *
     * @param programId the UUID of the program whose eligibility rules are to be retrieved.
     * @return a list of {@link EligibilityRuleDTO} objects for the specified program.
     *         Returns an empty list if the program has no eligibility rules defined.
     * @throws com.cts.fundtrack.common.exceptions.ProgramNotFoundException if no program
     *         exists with the given {@code programId}.
     */
    List<EligibilityRuleDTO> getRulesByProgramId(UUID programId);

    /**
     * Retrieves the full set of requirements (eligibility rules and required documents)
     * for a given program.
     *
     * <p>This method is primarily intended for internal use by the Application Service
     * via a Feign client, to validate an applicant's submission against all program criteria
     * in a single call.</p>
     *
     * @param programId the UUID of the program whose requirements are to be retrieved.
     * @return a {@link ProgramRequirementsDTO} containing the program name, the list of
     *         eligibility rules, and the list of required document names.
     * @throws com.cts.fundtrack.common.exceptions.ProgramNotFoundException if no program
     *         exists with the given {@code programId}.
     */
    ProgramRequirementsDTO getRequirements(UUID programId);

}
