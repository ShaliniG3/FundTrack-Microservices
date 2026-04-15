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

@RestController
@RequestMapping("/api/v1/reports")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Grant Reporting", description = "Endpoints for submitting grant progress reports.")
public class GrantReportController {

    private final GrantReportService grantReportService;

    /**
     * Submit a report. 
     * Restricted to APPLICANT. 
     * Note: #dto.applicationId requires the SecurityService bean to verify ownership.
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
     * Retrieve report history.
     * Accessible by Officers/Admins, or the APPLICANT who owns the application.
     */
    @Operation(summary = "Retrieve report history")
    @GetMapping("/grant_reports/{applicationId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN') or " +
                  "(hasRole('APPLICANT') and @securityService.isApplicationOwner(#applicationId))")
    public ResponseEntity<List<GrantReportResponseDTO>> getMyGrantReports(
            @PathVariable UUID applicationId) {

        log.info("Fetching reports for ApplicationID: {}", applicationId);

        List<GrantReportResponseDTO> history = grantReportService.getMyGrantReports(applicationId);
        List<GrantReportResponseDTO> safeHistory = (history != null) ? history : List.of();

        return ResponseEntity.ok(safeHistory);
    }
}