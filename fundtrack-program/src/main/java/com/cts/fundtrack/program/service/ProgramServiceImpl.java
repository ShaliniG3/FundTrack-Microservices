package com.cts.fundtrack.program.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.dto.EligibilityRuleDTO;
import com.cts.fundtrack.common.dto.ProgramRequestDTO;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;
    private final ProgramMapper programMapper;
    private final HttpServletRequest request; // For role-checks from Gateway headers

    /**
     * Determines if the current request is from an Applicant.
     * The Gateway injects the role into the 'X-User-Role' header.
     */
    private boolean isApplicant() {
        String role = request.getHeader("X-User-Role");
        return "ROLE_APPLICANT".equalsIgnoreCase(role);
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.CREATE, entityName = EntityType.PROGRAM)
    public ProgramResponseDTO createProgram(ProgramRequestDTO dto) {
        if (dto == null) throw new InvalidProgramStateException("Program data cannot be null.");
        validateDates(dto);

        Program program = programMapper.toEntity(dto);
        program.setStatus(ProgramStatus.DRAFT);

        // Explicitly set the parent reference for JPA cascading
        if (program.getEligibilityRules() != null) {
            program.getEligibilityRules().forEach(rule -> rule.setProgram(program));
        }
        if (program.getRequiredDocuments() != null) {
            program.getRequiredDocuments().forEach(docs -> docs.setProgram(program));
        }

        Program savedProgram = programRepository.save(program);
        return programMapper.toResponseDTO(savedProgram);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> getAllPrograms() {
        List<Program> programs;
        if (isApplicant()) {
            // Applicants only see Active/Closed
            programs = programRepository.findByStatusIn(Arrays.asList(ProgramStatus.ACTIVE, ProgramStatus.CLOSED));
        } else {
            // Internal Staff see everything (Draft, Archived, etc.)
            programs = programRepository.findAll();
        }

        return programs.stream()
                .map(programMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramResponseDTO getProgramById(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found with ID: " + programId));

        if (isApplicant() && !(program.getStatus() == ProgramStatus.ACTIVE || program.getStatus() == ProgramStatus.CLOSED)) {
            log.warn("Unauthorized access attempt to program: {}", programId);
            throw new AccessDeniedException("You do not have permission to view this program.");
        }

        return programMapper.toResponseDTO(program);
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.PROGRAM)
    public ProgramResponseDTO updateProgram(UUID programId, ProgramRequestDTO dto) {
        Program existingProgram = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found with ID: " + programId));

        // Update basic fields
        if (dto.getName() != null) existingProgram.setName(dto.getName());
        if (dto.getDescription() != null) existingProgram.setDescription(dto.getDescription());
        if (dto.getBudget() != null) existingProgram.setBudget(dto.getBudget());
        if (dto.getStartDate() != null) existingProgram.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) existingProgram.setEndDate(dto.getEndDate());

        validateDates(dto);
        return programMapper.toResponseDTO(programRepository.save(existingProgram));
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.PROGRAM)
    public void archiveProgram(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found."));
        program.setStatus(ProgramStatus.ARCHIVED);
        programRepository.save(program);
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.DELETE, entityName = EntityType.PROGRAM)
    public void deleteProgram(UUID programId) {
        if (!programRepository.existsById(programId)) {
            throw new ProgramNotFoundException("Cannot delete. Program not found.");
        }
        programRepository.deleteById(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponseDTO> searchPrograms(String keyWord) {
        List<Program> list;
        if (isApplicant()) {
            list = programRepository.searchByKeywordAndStatusIn(keyWord, 
                    Arrays.asList(ProgramStatus.ACTIVE, ProgramStatus.CLOSED));
        } else {
            list = programRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyWord, keyWord);
        }

        return list.stream()
                .map(programMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(action = ActionType.UPDATE, entityName = EntityType.PROGRAM)
    public ProgramResponseDTO updateProgramStatus(UUID programId, ProgramStatus status) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found."));

        // Business Rule: Cannot activate without criteria
        if (status == ProgramStatus.ACTIVE) {
            if (program.getEligibilityRules().isEmpty() || program.getRequiredDocuments().isEmpty()) {
                throw new InvalidProgramStateException("Program must have rules and document requirements before activation.");
            }
        }

        program.setStatus(status);
        return programMapper.toResponseDTO(programRepository.save(program));
    }

    private void validateDates(ProgramRequestDTO dto) {
        if (dto.getStartDate() != null && dto.getEndDate() != null &&
                dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new InvalidProgramStateException("End date cannot be before start date.");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<EligibilityRuleDTO> getRulesByProgramId(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found: " + programId));

        return program.getEligibilityRules().stream()
                .map(rule -> EligibilityRuleDTO.builder()
                        .ruleId(rule.getRuleId())
                        .ruleDescription(rule.getRuleDescription())
                        .ruleExpression(rule.getRuleExpression())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramRequirementsDTO getRequirements(UUID programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found: " + programId));

        // Get the Formulas (Objects)
        List<EligibilityRuleDTO> ruleDTOs = getRulesByProgramId(programId);

        // Get the Checklist (Strings)
        List<String> docNames = program.getRequiredDocuments().stream()
                .map(RequiredDocument::getName) // Maps the 'name' field from your entity
                .collect(Collectors.toList());

        return ProgramRequirementsDTO.builder()
                .programId(programId)
                .programName(program.getName())
                .rules(ruleDTOs)
                .requiredDocuments(docNames)
                .build();
    }
}