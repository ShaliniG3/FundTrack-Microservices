package com.cts.fundtrack.disbursement.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // ACTIVATED
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.DisbursementRequestDynamicDTO;
import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.disbursement.mapper.ModuleMapper;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.service.DisbursementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/disbursements")
@Tag(name = "Disbursement Management", description = "Finance-restricted endpoints for budget splitting and payment scheduling")
public class DisbursementController {

    private final DisbursementService disbursementService;
    private final ModuleMapper mapper;

    /**
     * Fetch Payment Schedule. Restricted to Finance Officers.
     */
    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(
            summary = "Fetch Payment Schedule",
            description = "Retrieves all scheduled installments for a specific application.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Requires FINANCE_OFFICER role")
            }
    )
    public ResponseEntity<List<DisbursementResponseDTO>> getScheduleByApplication(@PathVariable UUID applicationId) {
        log.debug("REST request to fetch schedule for application ID: {}", applicationId);
        
        List<Disbursement> res = disbursementService.getScheduleById(applicationId);
        List<DisbursementResponseDTO> response = res.stream()
                .map(mapper::toDisbursementResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Calculate Unpaid Balance. Restricted to Finance Officers.
     */
    @GetMapping("/application/{applicationId}/remaining-balance")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(summary = "Calculate Unpaid Balance")
    public ResponseEntity<Double> getRemainingBalance(@PathVariable UUID applicationId) {
        log.info("REST request to calculate remaining balance for application: {}", applicationId);
        Double balance = disbursementService.getRemainingBalance(applicationId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Batch Create Installments. The core "Budget Split" logic.
     */
    @PostMapping("/finalize-payment")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(
            summary = "Batch Create Installments",
            description = "Triggers the core budget-splitting algorithm for a closed program."
    )
    public ResponseEntity<List<DisbursementResponseDTO>> createScheduleDynamic(
            @Valid @RequestBody DisbursementRequestDynamicDTO dto) {

        log.info("REST request to finalize and split budget for program: {}", dto.getProgramId());
        
        List<DisbursementResponseDTO> response = disbursementService.finalizeAndSplitBudget(
                dto.getProgramId(),
                dto.getFrequency(),
                dto.getNumberOfPayments()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}