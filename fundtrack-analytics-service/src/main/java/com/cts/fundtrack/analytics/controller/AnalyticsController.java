package com.cts.fundtrack.analytics.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.analytics.service.AnalyticsService;
import com.cts.fundtrack.common.dto.DailyAnalysisDTO;
import com.cts.fundtrack.common.dto.FinanceSummaryDTO;
import com.cts.fundtrack.common.dto.StatusDistributionDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for the Analytics Microservice.
 * <p>
 * This controller provides endpoints for program-level metrics and financial summaries.
 * Authentication and Authorization are offloaded to the API Gateway; however, 
 * this service expects a valid 'loggedInUser' header for auditing purposes.
 * </p>
 * * @author Gemini
 * @version 2026.1
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Endpoints for program metrics and financial data aggregation")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Retrieves the status distribution for a specified program.
     *
     * @param programId the UUID of the program to analyze.
     * @return a {@link StatusDistributionDTO} containing categorized application counts.
     */
    @Operation(
        summary = "Get Status Distribution", 
        description = "Aggregates application counts by status (e.g., APPROVED, REJECTED) for a program."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved distribution",
            content = @Content(schema = @Schema(implementation = StatusDistributionDTO.class))),
        @ApiResponse(responseCode = "404", description = "Program or data not found"),
        @ApiResponse(responseCode = "500", description = "Internal error during data aggregation")
    })
    @GetMapping("/status-distribution/{programId}")
    public ResponseEntity<StatusDistributionDTO> getStatusDistribution(
            @Parameter(description = "The unique ID of the program", required = true)
            @PathVariable UUID programId) {
        
        log.info("Fetching status distribution for program: {}", programId);
        return ResponseEntity.ok(analyticsService.getStatusDistribution(programId));
    }

    /**
     * Retrieves daily application trends for a program.
     *
     * @param programId the UUID of the program.
     * @return a list of {@link DailyAnalysisDTO} objects representing daily metrics.
     */
    @Operation(
        summary = "Get Daily Analysis", 
        description = "Returns a time-series list of application counts grouped by submission date."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved daily metrics",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DailyAnalysisDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Request must come via API Gateway")
    })
    @GetMapping("/daily-analysis/{programId}")
    public ResponseEntity<List<DailyAnalysisDTO>> getDailyAnalysis(
            @Parameter(description = "The unique ID of the program", required = true)
            @PathVariable UUID programId) {
        
        log.info("Fetching daily analysis for program: {}", programId);
        return ResponseEntity.ok(analyticsService.getDailyAnalysis(programId));
    }

    /**
     * Provides a high-level financial summary and budget utilization report.
     *
     * @param programId the UUID of the program.
     * @return a {@link FinanceSummaryDTO} with budget and disbursement metrics.
     */
    @Operation(
        summary = "Get Finance Summary", 
        description = "Provides budget totals, disbursed funds, and utilization percentages."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully generated financial report",
            content = @Content(schema = @Schema(implementation = FinanceSummaryDTO.class))),
        @ApiResponse(responseCode = "503", description = "Downstream service (Finance/Grant) unavailable")
    })
    @GetMapping("/finance-summary/{programId}")
    public ResponseEntity<FinanceSummaryDTO> getFinanceSummary(
            @Parameter(description = "The unique ID of the program", required = true)
            @PathVariable UUID programId) {
        
        log.info("Generating finance summary for program: {}", programId);
        return ResponseEntity.ok(analyticsService.getFinanceSummary(programId));
    }
}