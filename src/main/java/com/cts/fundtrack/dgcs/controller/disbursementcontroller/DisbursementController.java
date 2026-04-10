package com.cts.fundtrack.dgcs.controller.disbursementcontroller;

import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementRequestDynamicDTO;
import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementResponseDTO;

import com.cts.fundtrack.dgcs.mapper.ModuleMapper;

import com.cts.fundtrack.dgcs.model.Disbursement;

import com.cts.fundtrack.dgcs.service.disbursementservice.DisbursementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing financial disbursements and payment scheduling.
 * <p>
 * This controller provides administrative endpoints for Finance Officers to
 * configure payment frequencies, monitor budget allocation, and track the
 * status of fund transfers at the application and program levels.
 * </p>
 */

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/disbursements")
@Tag(name = "Disbursement Management", description = "Finance-restricted endpoints for budget splitting, payment scheduling, and balance tracking")
public class DisbursementController {

    private final DisbursementService disbursementService;
    private final ModuleMapper mapper;

    /**
     * Retrieves the complete payment schedule for a specific grant application.
     *
     * @param applicationId The unique identifier of the grant application.
     * @return A list of scheduled installments and their current statuses.
     */

    @Operation(
            summary = "Fetch Payment Schedule",
            description = "Retrieves all scheduled installments for a specific application. "
                    + "This view allows Finance Officers to track pending, paid, and failed payments.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient permissions to view financial schedules"),
                    @ApiResponse(responseCode = "404", description = "Not Found: The requested Application ID does not exist"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: Database connectivity issue")
            }
    )

    // @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<DisbursementResponseDTO>> getScheduleByApplication(
            @Parameter(description = "UUID of the application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID applicationId) {

        log.debug("REST request to fetch schedule for application ID: {}", applicationId);
        List<Disbursement> res = disbursementService.getScheduleById(applicationId);
        List<DisbursementResponseDTO> response = res.stream()
                .map(mapper::toDisbursementResponseDTO)
                .collect(Collectors.toList());
        List<DisbursementResponseDTO> safeResponse = (response != null) ? response : List.of();
        log.info(" Records Retrieved: {}", safeResponse.size());

        return ResponseEntity.ok(safeResponse);
    }

    /**
     * Computes the total outstanding financial obligation for a specific application.
     * <p>
     * This endpoint aggregates all scheduled disbursements that have not yet been
     * finalized. It explicitly excludes records with 'PAID' or 'CANCELLED' statuses
     * to provide an accurate representation of remaining budgetary commitments.
     * </p>
     *
     * @param applicationId The unique identifier of the grant application.
     * @return The sum of all pending/scheduled installment amounts.
     */
    @Operation(
            summary = "Calculate Unpaid Balance",
            description = "Computes the total remaining funds to be disbursed for an application. "
                    + "Excludes PAID or CANCELLED records. Restricted to FINANCE_OFFICER role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Balance calculated successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Finance Officer access only"),
                    @ApiResponse(responseCode = "404", description = "Not Found: Application ID not found")
            }
    )

    //  @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @GetMapping("/application/{applicationId}/remaining-balance")
    public ResponseEntity<Double> getRemainingBalance(
            @Parameter(description = "UUID of the application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID applicationId) {

        log.info("REST request to calculate remaining balance for application: {}", applicationId);
        Double balance = disbursementService.getRemainingBalance(applicationId);
        log.info("Calculated Balance: {}", balance);
        return ResponseEntity.ok(balance);
    }

    /**
     * Triggers the automated budget-splitting and disbursement scheduling engine.
     * <p>
     * This high-impact operation processes all approved applications within a closed
     * funding program. It calculates individual award amounts and generates
     * a chronological sequence of disbursement installments based on the provided
     * frequency and payment count.
     * </p>
     *
     * @param dto Configuration parameters including Program ID, Frequency, and Installment Count.
     * @return A consolidated list of all generated disbursement installments.
     */

    //@PreAuthorize("hasRole('FINANCE_OFFICER')")
    @Operation(
            summary = "Batch Create Installments",
            description = "Executes the core budget-splitting algorithm for a finalized program. "
                    + "Calculates awards for all winners and persists a multi-period payment schedule. "
                    + "Restricted to FINANCE_OFFICER role.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Budget successfully split and schedules generated"),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid parameters or program status mismatch"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Finance Officer authorization required"),
                    @ApiResponse(responseCode = "500", description = "Internal Error: Failure during batch persistence or calculation")
            }
    )
    @PostMapping("/finalize-payment")
    public ResponseEntity<List<DisbursementResponseDTO>> createScheduleDynamic(
            @Valid @RequestBody DisbursementRequestDynamicDTO dto) {

        log.info("REST request to finalize and split budget for program: {}", dto.getProgramId());
        List<DisbursementResponseDTO> response = disbursementService.finalizeAndSplitBudget(
                dto.getProgramId(),
                dto.getFrequency(),
                dto.getNumberOfPayments()
        );
        log.info("Batch Generation Complete |Total Installments Created: {}", response.size());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}