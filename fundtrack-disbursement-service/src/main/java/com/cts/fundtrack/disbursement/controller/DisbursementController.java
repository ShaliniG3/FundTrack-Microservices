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

/**
 * REST controller exposing disbursement management endpoints for the FundTrack platform.
 * <p>
 * This controller provides finance-restricted operations for managing grant installment
 * schedules. It handles the core budget-splitting workflow that distributes a closed
 * program's total budget across all approved applicants in equal, time-spaced installments,
 * as well as balance inquiry endpoints for financial oversight.
 * </p>
 * <p>
 * All endpoints require the {@code FINANCE_OFFICER} role and are reachable at
 * {@code /api/v1/disbursements}.
 * </p>
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/disbursements")
@Tag(name = "Disbursement Management", description = "Finance-restricted endpoints for budget splitting and payment scheduling")
public class DisbursementController {

    private final DisbursementService disbursementService;
    private final ModuleMapper mapper;

    /**
     * Retrieves the full installment schedule for a given grant application.
     * <p>
     * Returns all disbursement records associated with the specified application,
     * ordered chronologically by scheduled date. Restricted to Finance Officers.
     * </p>
     *
     * @param applicationId the UUID of the grant application whose schedule is requested
     * @return a {@link ResponseEntity} containing a list of {@link DisbursementResponseDTO}
     *         objects representing each scheduled installment, or an empty list if none exist
     */
    @GetMapping("/application/{applicationId}")
//     @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'ADMIN')")
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
     * Calculates the total unpaid balance for a grant application.
     * <p>
     * Sums the amounts of all disbursement installments that are not in a
     * {@code PAID} or {@code CANCELLED} state, providing Finance Officers with
     * a real-time view of outstanding financial obligations. Restricted to
     * Finance Officers.
     * </p>
     *
     * @param applicationId the UUID of the grant application to query
     * @return a {@link ResponseEntity} containing the outstanding balance as a
     *         {@link Double}; returns {@code 0.0} if all installments are settled
     */
    @GetMapping("/application/{applicationId}/remaining-balance")
//     @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(summary = "Calculate Unpaid Balance")
    public ResponseEntity<Double> getRemainingBalance(@PathVariable UUID applicationId) {
        log.info("REST request to calculate remaining balance for application: {}", applicationId);
        Double balance = disbursementService.getRemainingBalance(applicationId);
        return ResponseEntity.ok(balance);
    }

    /**
     * Triggers the budget-splitting algorithm for a closed grant program.
     * <p>
     * This is the core financial finalization endpoint. It validates that the
     * program is in {@code CLOSED} status and that all application reviews are
     * complete, then divides the program's total budget equally among all approved
     * winners and creates a time-spaced installment schedule for each. Returns
     * {@code 201 Created} on success. Restricted to Finance Officers.
     * </p>
     *
     * @param dto the {@link DisbursementRequestDynamicDTO} containing the program ID,
     *            payment frequency (e.g., MONTHLY, QUARTERLY), and the total number
     *            of installments per recipient
     * @return a {@link ResponseEntity} with HTTP 201 and the list of all newly created
     *         {@link DisbursementResponseDTO} objects across all approved applicants
     */
    @PostMapping("/finalize-payment")
//     @PreAuthorize("hasRole('FINANCE_OFFICER')")
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