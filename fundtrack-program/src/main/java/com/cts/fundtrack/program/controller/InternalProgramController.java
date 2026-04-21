package com.cts.fundtrack.program.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.ProgramMetadataDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.program.service.ProgramService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal REST controller exposing lightweight program metadata for
 * consumption by peer microservices (e.g. the Disbursement Service) via Feign.
 *
 * <p>Base path: {@code /api/internal}</p>
 *
 * <p>These endpoints are NOT routed through the API Gateway to external clients.
 * They are called service-to-service with gateway-propagated security headers,
 * so no additional {@code @PreAuthorize} constraints are needed beyond the
 * general authentication requirement configured in {@code SecurityConfig}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalProgramController {

    private final ProgramService programService;

    /**
     * Returns lightweight metadata for a grant program, used by the Disbursement
     * Service to validate the program is {@code CLOSED} before splitting the budget.
     *
     * @param id the UUID of the program to retrieve
     * @return a {@link ProgramMetadataDTO} containing the program ID, name, status
     *         string, and total budget
     */
    @GetMapping("/programs/{id}")
    public ResponseEntity<ProgramMetadataDTO> getProgramMetadata(@PathVariable UUID id) {
        log.debug("Internal request for program metadata: {}", id);
        ProgramResponseDTO program = programService.getProgramById(id);
        ProgramMetadataDTO metadata = ProgramMetadataDTO.builder()
                .programId(program.getProgramId())
                .name(program.getName())
                .status(program.getStatus() != null ? program.getStatus().name() : null)
                .budget(program.getBudget())
                .build();
        return ResponseEntity.ok(metadata);
    }
}
