package com.cts.fundtrack.dgcs.controller.grantreportcontroller;

import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportRequestDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;

import com.cts.fundtrack.dgcs.service.grantreportservice.GrantReportService;

import com.cts.fundtrack.dgcs.exception.InvalidInputException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing grant progress report submissions and history.
 * <p>
 * Handles multipart requests containing JSON metadata and PDF evidence.
 * This controller facilitates the reporting lifecycle for applicants and
 * provides historical data access for audit personnel.
 * </p>
 */

@RestController
@RequestMapping("/api/v1/grant_report")
@Slf4j
@RequiredArgsConstructor
@Tag(
        name = "Grant Reporting",
        description = "Endpoints for submitting grant progress reports and retrieving report history."
)
public class GrantReportController {

    private final GrantReportService grantReportService;

    /**
     * Submits a new grant progress report along with a PDF proof document.
     *
     * @param dto   JSON metadata describing the report.
     * @param proof PDF document as supporting evidence.
     * @return Response containing stored report details.
     */
    @Operation(
            summary = "Submit grant progress report",
            description = "Accepts a multipart request that includes JSON metadata ('data') and a PDF file ('proof'). "
                    + "The PDF serves as supporting evidence for the grant's progress.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Report successfully submitted"),
                    @ApiResponse(responseCode = "400", description = "Validation failed: Check file type or mandatory fields"),
                    @ApiResponse(responseCode = "404", description = "Reference Error: The associated Application ID was not found"),
                    @ApiResponse(responseCode = "500", description = "Server Error: Disk storage or Database synchronization failure")
            }
    )

    // @PreAuthorize("hasRole('APPLICANT') and @securityService.isApplicationOwner(#dto.applicationId)")
    @PostMapping(value = "/my_report", consumes = {"multipart/form-data"})
    public ResponseEntity<GrantReportResponseDTO> submitGrantReport(
            @RequestPart("data") @Valid GrantReportRequestDTO dto,
            @RequestPart("proof") MultipartFile proof) {

        log.info("Ingress Request | POST /api/v1/reports/submit | ApplicationID: {} | FileName: {} | Size: {} bytes",
                dto.getApplicationId(), proof.getOriginalFilename(), proof.getSize());

        if (proof.getContentType() == null ||
                !proof.getContentType().equalsIgnoreCase(MediaType.APPLICATION_PDF_VALUE)) {
            log.error("Invalid MIME type for ApplicationID {}: {}", dto.getApplicationId(), proof.getContentType());
            throw new InvalidInputException("Only PDF files are permitted for submission.");
        }

        GrantReportResponseDTO response = grantReportService.submitGrantReport(dto, proof);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all progress reports submitted for a specific application.
     *
     * @param applicationId Identifier of the grant application.
     * @return List of submitted reports.
     */

    @Operation(
            summary = "Retrieve Report History",
            description = "Returns a complete list of all progress reports submitted for the specified application. "
                    + "Results include metadata and document paths for audit purposes. "
                    + "Restricted to Application Owners, Compliance Officers, and Admins.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report history successfully retrieved"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User is not authorized to access this history"),
                    @ApiResponse(responseCode = "404", description = "Not Found: The associated Application ID does not exist"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: Failed to resolve history from storage")
            }
    )

    // @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN') or " +
    //       "(hasRole('APPLICANT') and @securityService.isApplicationOwner(#applicationId))")
    @GetMapping("/my_reports/{applicationId}")
    public ResponseEntity<List<GrantReportResponseDTO>> getMyGrantReports(
            @PathVariable UUID applicationId) {

        log.info("Ingress Request | GET /api/v1/reports/{} | ApplicationID: {}", applicationId, applicationId);

        List<GrantReportResponseDTO> history = grantReportService.getMyGrantReports(applicationId);

        List<GrantReportResponseDTO> safeHistory = (history != null) ? history : List.of();

        log.info("Records Retrieved: {}", safeHistory.size());

        return ResponseEntity.ok(safeHistory);
    }
}