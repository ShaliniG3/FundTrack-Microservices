package com.cts.fundtrack.program.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
// Updated imports to point to common module and local service
import com.cts.fundtrack.common.dto.ProgramRequestDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.models.enums.ProgramStatus;
import com.cts.fundtrack.program.service.ProgramService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for the Program Microservice, exposing endpoints under {@code /api/v1/programs}.
 *
 * <p>This controller handles the full lifecycle of grant funding programs, including creation,
 * retrieval, update, status transitions, archival, and deletion. Security is enforced via
 * method-level {@code @PreAuthorize} annotations that rely on the {@code SecurityContext}
 * populated by the {@link com.cts.fundtrack.program.security.GatewayHeaderFilter}, which
 * converts gateway-injected HTTP headers into a Spring Security {@code Authentication} object.</p>
 *
 * <p>Role conventions:</p>
 * <ul>
 *   <li>{@code ADMIN} — can create, update, change status, archive, and delete programs.</li>
 *   <li>Any authenticated user — can view and search programs (with applicant-specific
 *       visibility restrictions applied at the service layer).</li>
 * </ul>
 *
 * <p>Two internal endpoints ({@code /{programId}/rules} and {@code /{programId}/requirements})
 * are also exposed for consumption by peer microservices (e.g., the Application Service)
 * via Feign clients, and do not require a specific role.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    /**
     * Creates a new grant funding program.
     *
     * <p>The program is persisted with an initial status of {@code DRAFT} and its
     * eligibility rules and required documents are linked in a single transaction.
     * A confirmation notification is dispatched via the Notification Service upon success.</p>
     *
     * @param dto the validated request body containing program details (name, description,
     *            budget, dates, eligibility rules, and required documents).
     * @return a {@link ResponseEntity} containing the created {@link ProgramResponseDTO}
     *         and HTTP status {@code 201 Created}.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProgramResponseDTO> createProgram(@Valid @RequestBody ProgramRequestDTO dto) {
        log.info("Microservice request to create program: {}", dto.getName());
        ProgramResponseDTO response = programService.createProgram(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves all grant funding programs visible to the authenticated user.
     *
     * <p>Applicants (role {@code APPLICANT}) only see programs in {@code ACTIVE} or
     * {@code CLOSED} status. Admin and staff users see all programs regardless of status.</p>
     *
     * @return a {@link ResponseEntity} containing a list of {@link ProgramResponseDTO} objects
     *         and HTTP status {@code 200 OK}.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ProgramResponseDTO>> getPrograms() {
        log.info("Microservice request to fetch all programs.");
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    /**
     * Retrieves a single grant funding program by its unique identifier.
     *
     * <p>Applicants are restricted from viewing programs in statuses other than
     * {@code ACTIVE} or {@code CLOSED}; an {@code AccessDeniedException} is thrown
     * at the service layer if this constraint is violated.</p>
     *
     * @param id the UUID of the program to retrieve.
     * @return a {@link ResponseEntity} containing the matching {@link ProgramResponseDTO}
     *         and HTTP status {@code 200 OK}.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> getProgramById(@PathVariable UUID id) {
        log.info("Microservice request to fetch program by ID: {}", id);
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    /**
     * Searches for grant funding programs by a keyword matched against program name and description.
     *
     * <p>The search is case-insensitive. Applicants only receive results for {@code ACTIVE}
     * or {@code CLOSED} programs; admin/staff searches span all statuses.</p>
     *
     * @param keyword the search term to match against program name and description fields.
     * @return a {@link ResponseEntity} containing a list of matching {@link ProgramResponseDTO}
     *         objects and HTTP status {@code 200 OK}.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<List<ProgramResponseDTO>> searchPrograms(@RequestParam String keyword) {
        log.info("Microservice request to search programs: {}", keyword);
        return ResponseEntity.ok(programService.searchPrograms(keyword));
    }

    /**
     * Updates the core details of an existing grant funding program.
     *
     * <p>Only non-null fields in the request body are applied to the existing entity.
     * The program status and ID remain unchanged by this operation.
     * A notification is dispatched upon successful update.</p>
     *
     * @param id  the UUID of the program to update.
     * @param dto the validated request body containing fields to update (name, description,
     *            budget, start/end dates). Null fields are ignored.
     * @return a {@link ResponseEntity} containing the updated {@link ProgramResponseDTO}
     *         and HTTP status {@code 200 OK}.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramRequestDTO dto) {
        log.info("Microservice request to update program: {}", id);
        return ResponseEntity.ok(programService.updateProgram(id, dto));
    }

    /**
     * Archives a grant funding program, setting its status to {@code ARCHIVED}.
     *
     * <p>Archiving is a soft deactivation — the program record is retained in the database
     * but is no longer accessible to applicants. A notification is dispatched on success.</p>
     *
     * @param id the UUID of the program to archive.
     * @return a {@link ResponseEntity} with HTTP status {@code 204 No Content} on success.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> archiveProgram(@PathVariable UUID id){
        log.info("Microservice request to archive program: {}", id);
        programService.archiveProgram(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the lifecycle status of a grant funding program.
     *
     * <p>Valid status transitions are enforced at the service layer. Notably, transitioning
     * to {@code ACTIVE} requires that the program has at least one eligibility rule and
     * one required document defined. A workflow notification is dispatched on success.</p>
     *
     * @param id     the UUID of the program whose status is to be changed.
     * @param status the target {@link ProgramStatus} (e.g., {@code ACTIVE}, {@code CLOSED},
     *               {@code ARCHIVED}).
     * @return a {@link ResponseEntity} containing the updated {@link ProgramResponseDTO}
     *         and HTTP status {@code 200 OK}.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProgramResponseDTO> updateProgramStatus(
            @PathVariable UUID id,
            @RequestParam ProgramStatus status) {
        log.info("Microservice request to update status of program {} to {}", id, status);
        return ResponseEntity.ok(programService.updateProgramStatus(id, status));
    }

    /**
     * Permanently deletes a grant funding program and all its associated data.
     *
     * <p>This is an irreversible hard-delete operation. It removes the program record and
     * all cascaded child records (eligibility rules, required documents) from the database.
     * A security confirmation notification is dispatched on success.</p>
     *
     * @param id the UUID of the program to permanently delete.
     * @return a {@link ResponseEntity} with HTTP status {@code 204 No Content} on success.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        log.info("Microservice request to permanently delete program: {}", id);
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Internal endpoint that returns all eligibility rules associated with a given program.
     *
     * <p>This endpoint is intended for consumption by the Application Service via a Feign
     * client. It is not surfaced to end users through the API Gateway and does not require
     * a specific role beyond network-level access.</p>
     *
     * @param programId the UUID of the program whose eligibility rules are to be retrieved.
     * @return a {@link ResponseEntity} containing a list of {@link EligibilityRuleDTO} objects
     *         and HTTP status {@code 200 OK}.
     */
    @GetMapping("/{programId}/rules")
    public ResponseEntity<List<EligibilityRuleDTO>> getRulesByProgramId(@PathVariable UUID programId) {
        return ResponseEntity.ok(programService.getRulesByProgramId(programId));
    }

    /**
     * Internal endpoint that returns the full requirements (rules and required documents)
     * for a given program.
     *
     * <p>This endpoint is intended for consumption by the Application Service via a Feign
     * client to validate applicant submissions against program criteria. It is not surfaced
     * to end users through the API Gateway.</p>
     *
     * @param programId the UUID of the program whose full requirements are to be retrieved.
     * @return a {@link ResponseEntity} containing a {@link ProgramRequirementsDTO} with
     *         the program name, eligibility rules, and required document names, and HTTP
     *         status {@code 200 OK}.
     */
    @GetMapping("/{programId}/requirements")
    public ResponseEntity<ProgramRequirementsDTO> getRequirements(@PathVariable UUID programId) {
        return ResponseEntity.ok(programService.getRequirements(programId));
    }
}
