package com.cts.fundtrack.disbursement.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // ACTIVATED
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.ApplicantComplianceDTO;
import com.cts.fundtrack.common.dto.ComplianceCheckRequestDTO;
import com.cts.fundtrack.common.dto.ComplianceHistoryDTO;
import com.cts.fundtrack.common.dto.GrantReportResponseDTO;
import com.cts.fundtrack.disbursement.service.ComplianceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(
        name = "Compliance & Audit",
        description = "Endpoints for grant auditing, compliance verification, and audit history retrieval."
)
public class ComplianceCheckController {

    private final ComplianceService complianceService;

    /**
     * Records an audit decision. Restricted to Officers and Admins.
     */
    @PostMapping("/audit")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(
            summary = "Record audit verdict",
            description = "Allows Compliance Officers to submit an approval or rejection decision.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Audit successfully recorded"),
                    @ApiResponse(responseCode = "403", description = "Access Denied"),
                    @ApiResponse(responseCode = "404", description = "Report not found")
            }
    )
    public ResponseEntity<String> recordAudit(@Valid @RequestBody ComplianceCheckRequestDTO dto) {
        log.info("Audit request received for ReportID: {} | Status: {}", dto.getGrantReportId(), dto.getStatus());
        String result = complianceService.recordAudit(dto);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves compliance dashboard. Restricted to Officers and Admins.
     */
    @GetMapping("/dashboard/program/{programId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Get compliance dashboard for a program")
    public ResponseEntity<List<ApplicantComplianceDTO>> getDashboardByProgram(@PathVariable UUID programId) {
        log.info("Fetching compliance dashboard for ProgramID: {}", programId);
        List<ApplicantComplianceDTO> dashboard = complianceService.getApplicantGrantReportingSummary(programId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Fetches audit history. Restricted to Officers and Admins.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Retrieve full audit history")
    public ResponseEntity<List<ComplianceHistoryDTO>> getAuditHistory(
            @RequestParam("complianceOfficerId") UUID complianceOfficerId) {
        log.info("Fetching compliance history for Officer ID: {}", complianceOfficerId);
        List<ComplianceHistoryDTO> history = complianceService.getComplianceHistoryByOfficer(complianceOfficerId);
        return ResponseEntity.ok(history);
    }

    /**
     * Identifies non-submitters. Open to Compliance, Finance, and Admins.
     */
    @GetMapping("/non-submitters/{programId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'FINANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Identify applicants with missing submissions")
    public ResponseEntity<List<ApplicantComplianceDTO>> getNonSubmitters(@PathVariable UUID programId) {
        log.info("Fetching non‑submitter list for ProgramID: {}", programId);
        List<ApplicantComplianceDTO> list = complianceService.getNonSubmittingApplicants(programId);
        return ResponseEntity.ok(list);
    }

    /**
     * Retrieves detailed report info. Restricted to Officers and Admins.
     */
    @GetMapping("/report/{reportId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Fetch report details")
    public ResponseEntity<GrantReportResponseDTO> getReportDetails(@PathVariable UUID reportId) {
        log.info("Fetching report details for ReportID: {}", reportId);
        GrantReportResponseDTO detail = complianceService.getGrantReportSummary(reportId);
        return ResponseEntity.ok(detail);
    }

    /**
     * Verification check for Finance Officers before disbursement.
     */
    @GetMapping("/compliance-check/{applicationId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'FINANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Check compliance readiness for disbursement")
    public ResponseEntity<Boolean> getGrantReportStatus(@PathVariable UUID applicationId) {
        log.info("Compliance check for ApplicationID: {}", applicationId);
        boolean isCompliant = complianceService.isApplicantCompliant(applicationId);
        return ResponseEntity.ok(isCompliant);
    }
}