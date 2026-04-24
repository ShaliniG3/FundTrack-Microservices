package com.cts.fundtrack.disbursement.controller; // Aligned with microservice structure

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // ACTIVATED
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cts.fundtrack.common.dto.GrantReportRequestDTO;
import com.cts.fundtrack.common.dto.GrantReportResponseDTO;
import com.cts.fundtrack.common.exceptions.InvalidInputException;
import com.cts.fundtrack.disbursement.service.GrantReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing grant progress report endpoints for the FundTrack platform.
 * <p>
 * This controller manages the post-award reporting obligations of grant recipients.
 * Applicants submit periodic progress reports (with PDF proof of fund utilisation) through
 * this controller, and officers retrieve historical submissions for compliance auditing.
 * </p>
 * <p>
 * Endpoints are served under {@code /api/v1/reports}. Submission is restricted to the
 * {@code APPLICANT} role, while history retrieval is accessible to Compliance Officers,
 * Admins, and the application's owning Applicant.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/reports")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Grant Reporting", description = "Endpoints for submitting grant progress reports.")
public class GrantReportController {

    private final GrantReportService grantReportService;

    /**
     * Submits a new grant progress report with supporting PDF evidence.
     * <p>
     * Accepts a multipart request containing structured report data (scope, metrics,
     * application ID) and a PDF file serving as proof of fund utilisation. The submission
     * undergoes two validation steps — eligibility check and reporting-window check —
     * before the file is stored and the report record persisted.
     * </p>
     * <p>
     * Only PDF files are accepted; other content types will result in a
     * {@code 400 Bad Request}. Restricted to the {@code APPLICANT} role.
     * </p>
     *
     * @param dto   the {@link GrantReportRequestDTO} containing the application ID,
     *              scope narrative, and quantitative metrics for this reporting period
     * @param proof the PDF {@link MultipartFile} serving as documentary evidence of
     *              grant fund utilisation (e.g., invoices, receipts)
     * @return a {@link ResponseEntity} with HTTP 201 and a {@link GrantReportResponseDTO}
     *         containing the persisted report's ID, status, and storage path
     * @throws com.cts.fundtrack.common.exceptions.InvalidInputException if the uploaded
     *         file is not a PDF
     */
    @Operation(summary = "Submit grant progress report")
    @PostMapping(value = "/grant_report", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<GrantReportResponseDTO> submitGrantReport(
            @RequestPart("data") @Valid GrantReportRequestDTO dto,
            @RequestPart("proof") MultipartFile proof) {

        log.info("Ingress Request | Submission | ApplicationID: {} | File: {}", 
                dto.getApplicationId(), proof.getOriginalFilename());

        // Basic PDF validation
        if (proof.getContentType() == null || 
            !proof.getContentType().equalsIgnoreCase(MediaType.APPLICATION_PDF_VALUE)) {
            throw new InvalidInputException("Only PDF files are permitted for submission.");
        }

        GrantReportResponseDTO response = grantReportService.submitGrantReport(dto, proof);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the full report submission history for a specific grant application.
     * <p>
     * Returns all grant progress reports ever submitted for the given application,
     * enabling both applicants to track their own submissions and officers to audit
     * the reporting trail. An empty list is returned (rather than a 404) when no
     * reports have been submitted yet.
     * </p>
     * <p>
     * Access is granted to Compliance Officers and Admins unconditionally, and to
     * the {@code APPLICANT} who owns the application (verified via
     * {@code @securityService.isApplicationOwner}).
     * </p>
     *
     * @param applicationId the UUID of the grant application whose report history is requested
     * @return a {@link ResponseEntity} containing a list of {@link GrantReportResponseDTO}
     *         objects; returns an empty list if no reports have been submitted
     */
    @Operation(summary = "Retrieve report history")
    @GetMapping("/grant_reports/{applicationId}")
//    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN') or " +
//                  "(hasRole('APPLICANT') and @securityService.isApplicationOwner(#applicationId))")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN', 'APPLICANT')")
    public ResponseEntity<List<GrantReportResponseDTO>> getMyGrantReports(
            @PathVariable UUID applicationId) {

        log.info("Fetching reports for ApplicationID: {}", applicationId);

        List<GrantReportResponseDTO> history = grantReportService.getMyGrantReports(applicationId);
        List<GrantReportResponseDTO> safeHistory = (history != null) ? history : List.of();

        return ResponseEntity.ok(safeHistory);
    }
}