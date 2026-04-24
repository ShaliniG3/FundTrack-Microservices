package com.cts.fundtrack.program.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.ProgramMetadataDTO;
import com.cts.fundtrack.common.exceptions.ProgramNotFoundException;
import com.cts.fundtrack.program.models.Program;
import com.cts.fundtrack.program.repository.ProgramRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal REST controller exposing program metadata endpoints exclusively for
 * service-to-service communication within the FundTrack microservice ecosystem.
 *
 * <p>Endpoints in this controller are not routed through the API Gateway and are
 * not accessible to external clients. They are intended solely for consumption
 * by peer microservices (e.g., Disbursement Service) via Feign clients.</p>
 *
 * <p>Critically, this controller bypasses the role-based visibility checks present
 * in {@link ProgramController} — specifically the {@code isApplicant()} header check
 * in {@code ProgramServiceImpl} — which would throw errors when called from Feign
 * since no {@code X-User-Role} header is present in internal service-to-service
 * requests.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/programs")
@RequiredArgsConstructor
public class ProgramInternalController {

    private final ProgramRepository programRepository;

    /**
     * Returns lightweight metadata for a single grant program, including its
     * name, total budget, and current lifecycle status.
     *
     * <p>This endpoint is consumed by the Disbursement Service to validate that
     * a program is in {@code CLOSED} status before initiating a budget split,
     * and to obtain the total budget for equal distribution among approved
     * applicants.</p>
     *
     * <p>Goes directly to the repository to avoid the HTTP header checks in
     * {@code ProgramServiceImpl#getProgramById()}, which require gateway-injected
     * headers that are not present in Feign calls.</p>
     *
     * @param id the UUID of the program whose metadata is to be retrieved
     * @return a {@link ResponseEntity} containing a {@link ProgramMetadataDTO}
     *         with the program's ID, name, budget, and status
     * @throws ProgramNotFoundException if no program exists with the given ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProgramMetadataDTO> getProgramMetadata(@PathVariable UUID id) {
        log.info("[Internal] Metadata request for program ID: {}", id);

        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ProgramNotFoundException("Program not found with ID: " + id));

        ProgramMetadataDTO metadata = ProgramMetadataDTO.builder()
                .programId(program.getProgramId())
                .name(program.getName())
                .budget(program.getBudget())
                .status(program.getStatus().name())
                .build();

        return ResponseEntity.ok(metadata);
    }
}