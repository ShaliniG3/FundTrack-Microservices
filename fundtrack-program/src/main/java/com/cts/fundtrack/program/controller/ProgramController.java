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
 * Microservice controller for managing funding programs.
 * Hooks into the Gateway-injected security context for Method Security.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProgramResponseDTO> createProgram(@Valid @RequestBody ProgramRequestDTO dto) {
        log.info("Microservice request to create program: {}", dto.getName());
        ProgramResponseDTO response = programService.createProgram(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ProgramResponseDTO>> getPrograms() {
        log.info("Microservice request to fetch all programs.");
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> getProgramById(@PathVariable UUID id) {
        log.info("Microservice request to fetch program by ID: {}", id);
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<List<ProgramResponseDTO>> searchPrograms(@RequestParam String keyword) {
        log.info("Microservice request to search programs: {}", keyword);
        return ResponseEntity.ok(programService.searchPrograms(keyword));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @PathVariable UUID id,
            @Valid @RequestBody ProgramRequestDTO dto) {
        log.info("Microservice request to update program: {}", id);
        return ResponseEntity.ok(programService.updateProgram(id, dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<Void> archiveProgram(@PathVariable UUID id){
        log.info("Microservice request to archive program: {}", id);
        programService.archiveProgram(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ProgramResponseDTO> updateProgramStatus(
            @PathVariable UUID id,
            @RequestParam ProgramStatus status) {
        log.info("Microservice request to update status of program {} to {}", id, status);
        return ResponseEntity.ok(programService.updateProgramStatus(id, status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        log.info("Microservice request to permanently delete program: {}", id);
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Internal endpoint for Application Service to fetch rules via Feign.
     */
    @GetMapping("/{programId}/rules")
    public ResponseEntity<List<EligibilityRuleDTO>> getRulesByProgramId(@PathVariable UUID programId) {
        return ResponseEntity.ok(programService.getRulesByProgramId(programId));
    }

    /**
     * Internal endpoint for Application Service to fetch full requirements via Feign.
     */
    @GetMapping("/{programId}/requirements")
    public ResponseEntity<ProgramRequirementsDTO> getRequirements(@PathVariable UUID programId) {
        return ResponseEntity.ok(programService.getRequirements(programId));
    }
}