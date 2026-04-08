package com.cts.fundtrack.dgcs.controller.disbursementcontroller;

import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementRequestDynamicDTO;
import com.cts.fundtrack.dgcs.dto.disbursementdto.DisbursementResponseDTO;
import com.cts.fundtrack.dgcs.mapper.ModuleMapper;
import com.cts.fundtrack.dgcs.model.Disbursement;
import com.cts.fundtrack.dgcs.service.disbursementservice.DisbursementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/disbursements")
@Tag(name = "Disbursement Management", description = "Finance-restricted endpoints for budget splitting, payment scheduling, and balance tracking")
public class DisbursementController {

    private final DisbursementService disbursementService;
    private final ModuleMapper mapper;

   // @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @GetMapping("/application/{applicationId}")
    @Operation(
            summary = "Fetch Payment Schedule",
            description = "Retrieves all scheduled installments for a specific application. Restricted to FINANCE_OFFICER.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DisbursementResponseDTO.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Requires FINANCE_OFFICER role"),
                    @ApiResponse(responseCode = "404", description = "Application not found")
            }
    )
    public ResponseEntity<List<DisbursementResponseDTO>> getScheduleByApplication(
            @Parameter(description = "UUID of the application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID applicationId) {

        log.debug("REST request to fetch schedule for application ID: {}", applicationId);
        List<Disbursement> res = disbursementService.getScheduleById(applicationId);
        List<DisbursementResponseDTO> response = res.stream()
                .map(mapper::toDisbursementResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

  //  @PreAuthorize("hasRole('FINANCE_OFFICER')")
    @GetMapping("/application/{applicationId}/remaining-balance")
    @Operation(
            summary = "Calculate Unpaid Balance",
            description = "Computes the total remaining funds to be disbursed for an application, excluding PAID or CANCELLED records.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Balance calculated successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Finance Officer access only")
            }
    )
    public ResponseEntity<Double> getRemainingBalance(
            @Parameter(description = "UUID of the application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID applicationId) {

        log.info("REST request to calculate remaining balance for application: {}", applicationId);
        Double balance = disbursementService.getRemainingBalance(applicationId);
        return ResponseEntity.ok(balance);
    }

    //@PreAuthorize("hasRole('FINANCE_OFFICER')")
    @PostMapping("/finalize-payment")
    @Operation(
            summary = "Batch Create Installments",
            description = "Triggers the core budget-splitting algorithm for a closed program. Splits the budget among winners and creates payment schedules.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Budget successfully split and schedules generated"),
                    @ApiResponse(responseCode = "400", description = "Invalid request or Program not in a state to be finalized"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Finance Officer access only")
            }
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