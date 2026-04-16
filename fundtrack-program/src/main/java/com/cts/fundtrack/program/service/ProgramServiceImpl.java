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
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.ProgramRequestDTO;
import com.cts.fundtrack.common.dto.ProgramRequirementsDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.exceptions.InvalidProgramStateException;
import com.cts.fundtrack.common.exceptions.ProgramNotFoundException;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.common.models.enums.ProgramStatus;
import com.cts.fundtrack.program.mapper.ProgramMapper;
import com.cts.fundtrack.program.models.Program;
import com.cts.fundtrack.program.models.RequiredDocument;
import com.cts.fundtrack.program.repository.ProgramRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing the lifecycle of funding programs.
 * <p>
 * This service provides administrative capabilities for creating, updating, 
 * and status-management of programs. It includes role-based filtering to ensure 
 * Applicants only view public-facing program data while Staff can manage internal DRAFTS.
 * </p>
 *
 * @author FundTrack Development Team
 * @version 1.2
 * @since 2026-04-16
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
     * Determines if the current request is originated from a user with the Applicant role.
     * @return true if the role is ROLE_APPLICANT.
     */
    private boolean isApplicant() {
        String role = request.getHeader("X-User-Role");
        return "ROLE_APPLICANT".equalsIgnoreCase(role);
    }

    /**
     * Extracts the Unique Identifier of the currently authenticated user from the request headers.
     * @return The UUID of the logged-in user.
     */
    private UUID getCurrentUserId() {
        String userIdStr = request.getHeader("X-User-Id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Initializes and persists a new funding program.
     * <p>
     * Sends a confirmation notification to the logged-in Admin upon successful creation.
     * </p>
     * @param dto Data transfer object containing program details.
     * @return The created program details as a {@link ProgramResponseDTO}.
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

        // Ensure bi-directional JPA relationship for cascading
        if (program.getEligibilityRules() != null) {
            program.getEligibilityRules().forEach(rule -> rule.setProgram(program));
        }
        if (program.getRequiredDocuments() != null) {
            program.getRequiredDocuments().forEach(docs -> docs.setProgram(program));
        }

        Program savedProgram = programRepository.save(program);
        
        // Notify the currently logged-in Admin/Staff
        sendInternalNotification(null, "Confirmation: You have successfully created the program '" + savedProgram.getName() + "'.", NotificationCategory.GENERAL);

        return programMapper.toResponseDTO(savedProgram);
    }

    /**
     * Retrieves all programs filtered by the user's role permissions.
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
     * Updates an existing program's metadata.
     * @param programId The target program ID.
     * @param dto Updated data.
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

        sendInternalNotification(programId, "Success: Program '" + saved.getName() + "' has been updated.", NotificationCategory.GENERAL);

        return programMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.PROGRAM)
    public void archiveProgram(UUID programId) {
        log.info("Archiving program ID: {}", programId);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found."));
        program.setStatus(ProgramStatus.ARCHIVED);
        programRepository.save(program);

        sendInternalNotification(programId, "System Alert: Program '" + program.getName() + "' is now ARCHIVED.", NotificationCategory.GENERAL);
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.PROGRAM)
    public void deleteProgram(UUID programId) {
        log.warn("PERMANENT DELETE triggered for program ID: {}", programId);
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Cannot delete. Program not found."));
        programRepository.deleteById(programId);

        sendInternalNotification(null, "Security Confirmation: Program '" + program.getName() + "' was permanently deleted.", NotificationCategory.GENERAL);
    }

    /**
     * Transitions a program to a new workflow status (e.g., DRAFT -> ACTIVE).
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

        sendInternalNotification(programId, "Workflow Update: Status for '" + saved.getName() + "' is now " + status, NotificationCategory.APPLICATION);

        return programMapper.toResponseDTO(saved);
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<EligibilityRuleDTO> getRulesByProgramId(UUID programId) {
        Program program = programRepository.findById(programId).orElseThrow(() -> new ProgramNotFoundException("Not found."));
        return program.getEligibilityRules().stream()
                .map(r -> EligibilityRuleDTO.builder().ruleId(r.getRuleId()).ruleDescription(r.getRuleDescription()).build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> searchPrograms(String keyWord) {
        List<Program> list = isApplicant() ? 
            programRepository.searchByKeywordAndStatusIn(keyWord, Arrays.asList(ProgramStatus.ACTIVE, ProgramStatus.CLOSED)) :
            programRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyWord, keyWord);

        return list.stream().map(programMapper::toResponseDTO).collect(Collectors.toList());
    }

    private void validateDates(ProgramRequestDTO dto) {
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new InvalidProgramStateException("Date Validation Error: End date cannot occur before start date.");
        }
    }

    /**
     * Internal dispatcher for inter-service notifications. 
     * Targets the currently logged-in user to confirm administrative actions.
     */
    private void sendInternalNotification(UUID appId, String message, NotificationCategory category) {
        UUID currentLoggedInUser = getCurrentUserId();
        
        if (currentLoggedInUser == null) {
            log.warn("Notification skipped: No authenticated user context found in headers.");
            return;
        }

        try {
            NotificationRequestDTO notification = NotificationRequestDTO.builder()
                    .userId(currentLoggedInUser) // 🚀 TARGETS THE LOGGED-IN USER
                    .applicationId(appId)
                    .message(message)
                    .category(category)
                    .build();
            notificationClient.sendNotification(notification);
            log.debug("Transactional confirmation sent to user: {}", currentLoggedInUser);
        } catch (Exception e) {
            log.error("Feign Error: Unable to reach Notification Service for user {}. Error: {}", currentLoggedInUser, e.getMessage());
        }
    }
}