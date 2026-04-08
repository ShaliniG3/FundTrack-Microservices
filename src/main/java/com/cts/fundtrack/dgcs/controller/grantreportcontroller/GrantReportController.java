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
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller providing endpoints for submitting and retrieving grant progress reports.
 * <p>
 * Supports multipart uploads where JSON metadata and a PDF proof document are submitted together.
 * Applicants use these endpoints to submit progress updates, while audit personnel may retrieve
 * previously submitted reports.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/reports")
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
                    @ApiResponse(responseCode = "400", description = "Invalid request or non-PDF uploaded"),
                    @ApiResponse(responseCode = "500", description = "Error while processing the request")
            }
    )

   // @PreAuthorize("hasRole('APPLICANT') and @securityService.isApplicationOwner(#dto.applicationId)")
    @PostMapping(value = "/submit", consumes = {"multipart/form-data"})
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
            summary = "Retrieve report history",
            description = "Returns all progress reports that have been previously submitted "
                    + "for the specified grant application."
    )

   // @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN') or " +
     //       "(hasRole('APPLICANT') and @securityService.isApplicationOwner(#applicationId))")
    @GetMapping("/my-report")
    public ResponseEntity<List<GrantReportResponseDTO>> getMyGrantReports(
            @RequestParam UUID applicationId) {

        log.info("Ingress Request | GET /api/v1/reports/my-report | ApplicationID: {}", applicationId);

        List<GrantReportResponseDTO> history = grantReportService.getMyGrantReports(applicationId);

        log.info("Records Retrieved: {}", history.size());
        return ResponseEntity.ok(history);
    }
}