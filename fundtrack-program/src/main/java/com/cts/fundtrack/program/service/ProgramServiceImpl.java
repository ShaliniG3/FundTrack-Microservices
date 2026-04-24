package com.cts.fundtrack.program.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.NotificationClient;
import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequestDTO;
import com.cts.fundtrack.common.dto.SimpleNotificationRequestDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.exceptions.InvalidProgramStateException;
import com.cts.fundtrack.common.exceptions.ProgramNotFoundException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.ProgramStatus;
import com.cts.fundtrack.program.mapper.ProgramMapper;
import com.cts.fundtrack.program.models.Program;
import com.cts.fundtrack.program.models.RequiredDocument;
import com.cts.fundtrack.program.repository.ProgramRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Primary implementation of {@link ProgramService}, providing the complete business logic
 * for managing grant funding programs within the FundTrack system.
 *
 * <p>This service orchestrates the following responsibilities:</p>
 * <ul>
 *   <li>Persistence — delegates to {@link ProgramRepository} for all database operations.</li>
 *   <li>Mapping — uses {@link ProgramMapper} (MapStruct) to convert between JPA entities
 *       and DTOs.</li>
 *   <li>Security — reads gateway-injected HTTP headers ({@code X-User-Role},
 *       {@code X-User-Id}) to apply role-based visibility rules (e.g., restricting
 *       applicants to {@code ACTIVE}/{@code CLOSED} programs).</li>
 *   <li>Audit logging — methods annotated with {@link Auditable} are intercepted by
 *       {@link com.cts.fundtrack.program.aspect.AuditAspect}, which publishes audit
 *       events to the Identity/Audit Service via Feign.</li>
 *   <li>Notifications — dispatches transactional notifications to the currently
 *       authenticated user via the {@link NotificationClient} Feign client after each
 *       significant state-changing operation.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;
    private final HttpServletRequest request;
    private final NotificationClient notificationClient;

    /**
     * Determines whether the currently authenticated user has the {@code APPLICANT} role.
     *
     * <p>The role is read from the {@code X-User-Role} HTTP header, which is injected by
     * the API Gateway after validating the user's JWT. This check drives visibility
     * restrictions throughout the service (e.g., filtering programs by status).</p>
     *
     * @return {@code true} if the {@code X-User-Role} header value is {@code ROLE_APPLICANT}
     *         (case-insensitive); {@code false} otherwise or if the header is absent.
     */
    private boolean isApplicant() {
        String role = request.getHeader("X-User-Role");
        return "ROLE_APPLICANT".equalsIgnoreCase(role);
    }

    /**
     * Extracts the UUID of the currently authenticated user from the request headers.
     *
     * <p>The user ID is read from the {@code X-User-Id} HTTP header, which is injected
     * by the API Gateway. This value is used when building notification and audit payloads.</p>
     *
     * @return the {@link UUID} of the current user, or {@code null} if the
     *         {@code X-User-Id} header is absent or blank.
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: the program is mapped from the DTO, forced to {@code DRAFT}
     * status, and all child eligibility rules and required documents have their back-reference
     * to the parent {@link Program} set before the entity graph is persisted in one shot.</p>
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.PROGRAM)
    public ProgramResponseDTO createProgram(ProgramRequestDTO dto) {
        log.info("Initiating creation of new program: {}", dto.getName());
        if (dto == null) throw new InvalidProgramStateException("Program data cannot be null.");
        validateDates(dto);

        Program program = programMapper.toEntity(dto);
        program.setStatus(ProgramStatus.DRAFT);

        if (program.getEligibilityRules() != null) {
            program.getEligibilityRules().forEach(rule -> rule.setProgram(program));
        }
        if (program.getRequiredDocuments() != null) {
            program.getRequiredDocuments().forEach(docs -> docs.setProgram(program));
        }

        Program savedProgram = programRepository.save(program);

        sendSimpleNotification("Confirmation: You have successfully created the program '" + savedProgram.getName() + "'.");

        return programMapper.toResponseDTO(savedProgram);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: applicant visibility is determined by {@link #isApplicant()},
     * which inspects the {@code X-User-Role} gateway header. Applicants receive only
     * {@code ACTIVE} and {@code CLOSED} programs; all other roles receive the full list.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> getAllPrograms() {
        log.debug("Fetching program list. Role-restricted: {}", isApplicant());
        List<Program> programs;
        if (isApplicant()) {
            programs = programRepository.findByStatusIn(Arrays.asList(ProgramStatus.ACTIVE, ProgramStatus.CLOSED));
        } else {
            programs = programRepository.findAll();
        }
        return programs.stream().map(programMapper::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: after the program is fetched, an additional role-based
     * check is applied. If the caller is an applicant and the program's status is neither
     * {@code ACTIVE} nor {@code CLOSED}, an {@link AccessDeniedException} is thrown to
     * prevent information leakage about programs in other lifecycle states.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public ProgramResponseDTO getProgramById(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found with ID: " + programId));

        if (isApplicant() && !(program.getStatus() == ProgramStatus.ACTIVE || program.getStatus() == ProgramStatus.CLOSED)) {
            log.warn("Access Denied: User attempted to view restricted program state: {}", programId);
            throw new AccessDeniedException("You do not have permission to view this program.");
        }
        return programMapper.toResponseDTO(program);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: only non-null scalar fields (name, description, budget,
     * startDate, endDate) from the DTO are applied to the existing entity. Nested
     * collections (eligibility rules, required documents) are not modified by this method;
     * use the status update flow for lifecycle changes.</p>
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.PROGRAM)
    public ProgramResponseDTO updateProgram(UUID programId, ProgramRequestDTO dto) {
        log.info("Updating program ID: {}", programId);
        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found with ID: " + programId));

        if (dto.getName() != null) existingProgram.setName(dto.getName());
        if (dto.getDescription() != null) existingProgram.setDescription(dto.getDescription());
        if (dto.getBudget() != null) existingProgram.setBudget(dto.getBudget());
        if (dto.getStartDate() != null) existingProgram.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) existingProgram.setEndDate(dto.getEndDate());

        validateDates(dto);
        Program saved = programRepository.save(existingProgram);

        sendSimpleNotification("Success: Program '" + saved.getName() + "' has been updated.");

        return programMapper.toResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: the program's status field is set to {@code ARCHIVED}
     * and the entity is saved. No cascading changes are made to child records.</p>
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.PROGRAM)
    public void archiveProgram(UUID programId) {
        log.info("Archiving program ID: {}", programId);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found."));
        program.setStatus(ProgramStatus.ARCHIVED);
        programRepository.save(program);

        sendSimpleNotification("System Alert: Program '" + program.getName() + "' is now ARCHIVED.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: uses {@link ProgramRepository#deleteById(Object)} which
     * triggers cascaded deletion of all child {@code EligibilityRule} and
     * {@code RequiredDocument} records via JPA cascade settings on the {@link Program} entity.</p>
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.PROGRAM)
    public void deleteProgram(UUID programId) {
        log.warn("PERMANENT DELETE triggered for program ID: {}", programId);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Cannot delete. Program not found."));
        programRepository.deleteById(programId);

        sendSimpleNotification("Security Confirmation: Program '" + program.getName() + "' was permanently deleted.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: activation to {@code ACTIVE} is guarded — the program must
     * have at least one eligibility rule and one required document, otherwise an
     * {@link InvalidProgramStateException} is thrown. No guard is applied for other status
     * transitions (e.g., {@code CLOSED}, {@code ARCHIVED}).</p>
     */
    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.PROGRAM)
    public ProgramResponseDTO updateProgramStatus(UUID programId, ProgramStatus status) {
        log.info("Transitioning program {} to status {}", programId, status);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found."));

        if (status == ProgramStatus.ACTIVE) {
            if (program.getEligibilityRules().isEmpty() || program.getRequiredDocuments().isEmpty()) {
                throw new InvalidProgramStateException("Activation Blocked: Programs must have eligibility criteria and document requirements defined.");
            }
        }

        program.setStatus(status);
        Program saved = programRepository.save(program);

        sendSimpleNotification("Workflow Update: Status for '" + saved.getName() + "' is now " + status + ".");

        return programMapper.toResponseDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: eligibility rules are fetched via the program's in-memory
     * collection (loaded from the {@code EAGER}/{@code LAZY} join), then projected into
     * {@link EligibilityRuleDTO} instances using a builder stream rather than the mapper,
     * to avoid loading unnecessary associations.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public ProgramRequirementsDTO getRequirements(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found."));

        List<EligibilityRuleDTO> rules = program.getEligibilityRules().stream()
                .map(r -> EligibilityRuleDTO.builder()
                        .ruleId(r.getRuleId())
                        .ruleDescription(r.getRuleDescription())
                        .ruleExpression(r.getRuleExpression()).build())
                .collect(Collectors.toList());

        List<String> docs = program.getRequiredDocuments().stream().map(RequiredDocument::getName).collect(Collectors.toList());

        return ProgramRequirementsDTO.builder()
                .programId(programId).programName(program.getName()).rules(rules).requiredDocuments(docs).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: returns only the eligibility rules for the given program,
     * projected directly from the program entity's collection into {@link EligibilityRuleDTO}
     * instances via a builder stream.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<EligibilityRuleDTO> getRulesByProgramId(UUID programId) {
        Program program = programRepository.findById(programId).orElseThrow(() -> new ProgramNotFoundException("Not found."));
        return program.getEligibilityRules().stream()
                .map(r -> EligibilityRuleDTO.builder().ruleId(r.getRuleId()).ruleDescription(r.getRuleDescription()).ruleExpression(r.getRuleExpression()).build())
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation detail: the keyword search is delegated to different repository
     * methods depending on the caller's role. Applicants use a JPQL query that filters by
     * status; admin/staff use Spring Data's derived query across name and description fields.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> searchPrograms(String keyWord) {
        List<Program> list = isApplicant() ?
            programRepository.searchByKeywordAndStatusIn(keyWord, Arrays.asList(ProgramStatus.ACTIVE, ProgramStatus.CLOSED)) :
            programRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyWord, keyWord);

        return list.stream().map(programMapper::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * Validates that the end date in the given request DTO does not precede its start date.
     *
     * <p>Both dates must be non-null for the comparison to be performed. If only one date
     * is present, no validation error is raised.</p>
     *
     * @param dto the request DTO whose {@code startDate} and {@code endDate} fields are validated.
     * @throws InvalidProgramStateException if {@code endDate} is strictly before {@code startDate}.
     */
    private void validateDates(ProgramRequestDTO dto) {
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new InvalidProgramStateException("Date Validation Error: End date cannot occur before start date.");
        }
    }

    /**
     * Dispatches an in-app notification to the currently authenticated user via the
     * Notification Service Feign client.
     *
     * <p>The method is intentionally non-fatal: any exception thrown by the Feign call
     * (e.g., network timeout, service unavailability) is caught and logged as an error
     * without propagating, so that a notification failure never rolls back the main
     * business transaction.</p>
     *
     * <p>If no authenticated user context is found in the gateway headers (i.e.,
     * {@code X-User-Id} is absent), the notification is silently skipped.</p>
     *
     * @param appId    an optional application UUID to associate with the notification
     *                 (may be {@code null} for program-level events).
     * @param message  the human-readable notification message to deliver.
     * @param category the {@link NotificationCategory} classifying the notification type.
     */
    private void sendSimpleNotification(String message) {
        UUID currentLoggedInUser = getCurrentUserId();

        if (currentLoggedInUser == null) {
            log.warn("Notification skipped: No authenticated user context found in headers.");
            return;
        }

        try {
            SimpleNotificationRequestDTO notification = new SimpleNotificationRequestDTO();
            notification.setUserId(currentLoggedInUser);
            notification.setMessage(message);
            notificationClient.sendSimpleNotification(notification);
            log.debug("Notification sent to user: {}", currentLoggedInUser);
        } catch (Exception e) {
            log.error("Feign Error: Unable to reach Notification Service for user {}. Error: {}", currentLoggedInUser, e.getMessage());
        }
    }
}
