package com.cts.fundtrack.dgcs.controller.compliancecontroller;

import com.cts.fundtrack.dgcs.dto.compliancedto.ApplicantComplianceDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckRequestDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceHistoryDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;
import com.cts.fundtrack.dgcs.service.complianceservice.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller providing compliance and audit functionalities for grant programs.
 * <p>
 * Exposes endpoints for Compliance Officers and Administrators to record audit decisions,
 * view program‑level compliance dashboards, retrieve audit history, and identify
 * applicants with missing submissions.
 * </p>
 */
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
     * Records an audit decision (approval/rejection) for a grant report.
     *
     * @param dto Audit verdict details including report ID and officer remarks.
     * @return Confirmation message.
     */
    //@PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @PostMapping("/audit")
    @Operation(
            summary = "Record audit verdict",
            description = "Allows Compliance Officers to submit an approval or rejection decision for a grant report.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Audit successfully recorded"),
                    @ApiResponse(responseCode = "400", description = "Invalid audit request data"),
                    @ApiResponse(responseCode = "404", description = "Report not found")
            }
    )
    public ResponseEntity<String> recordAudit(@Valid @RequestBody ComplianceCheckRequestDTO dto) {
        log.info("Audit request received for ReportID: {} | Status: {}", dto.getGrantReportId(), dto.getStatus());

        String result = complianceService.recordAudit(dto);

        log.info("Audit saved | ReportID: {} | Result: {}", dto.getGrantReportId(), result);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves a compliance dashboard for a specific program.
     *
     * @param programId Program identifier.
     * @return List of applicant compliance summaries.
     */
   // @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @GetMapping("/dashboard/program/{programId}")
    @Operation(
            summary = "Get compliance dashboard for a program",
            description = "Returns compliance summaries for all applications under a specific program.",
            parameters = @Parameter(name = "programId", description = "UUID of the grant program")
    )
    public ResponseEntity<List<ApplicantComplianceDTO>> getDashboardByProgram(@PathVariable UUID programId) {
        log.info("Fetching compliance dashboard for ProgramID: {}", programId);

        List<ApplicantComplianceDTO> dashboard = complianceService.getApplicantGrantReportingSummary(programId);

        log.info("Dashboard generated | Count: {}", dashboard.size());
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Fetches complete audit history across the system.
     *
     * @return List of audit records.
     */
   // @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @GetMapping("/history")
    @Operation(
            summary = "Retrieve full audit history",
            description = "Returns all recorded compliance actions across the system."
    )
    public ResponseEntity<List<ComplianceHistoryDTO>> getAuditHistory(
            @Parameter(description = "UUID of the compliance officer", required = true)
            @RequestParam("complianceOfficerId") UUID complianceOfficerId) {

        log.info("REST Request: Fetching compliance history for Officer ID: {}", complianceOfficerId);

        // Calling the updated service method we created
        List<ComplianceHistoryDTO> history = complianceService.getComplianceHistoryByOfficer(complianceOfficerId);

        log.info("REST Response: Found {} records for Officer ID: {}", history.size(), complianceOfficerId);

        return ResponseEntity.ok(history);
    }

    /**
     * Identifies applicants who have missed required report submissions.
     *
     * @param programId Target program identifier.
     * @return List of applicants marked as non‑submitters.
     */
//    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','FINANCE_OFFICER', 'ADMIN')")
    @GetMapping("/non-submitters/{programId}")
    @Operation(
            summary = "Identify applicants with missing submissions",
            description = "Lists applicants who are overdue for progress report submissions."
    )
    public ResponseEntity<List<ApplicantComplianceDTO>> getNonSubmitters(@PathVariable UUID programId) {
        log.info("Fetching non‑submitter list for ProgramID: {}", programId);

        List<ApplicantComplianceDTO> list = complianceService.getNonSubmittingApplicants(programId);

        log.info("Found {} non‑submitters", list.size());
        return ResponseEntity.ok(list);
    }
//
//    /**
//     * Retrieves detailed information for a specific grant report.
//     *
//     * @param reportId Report identifier.
//     * @return Report details for audit review.
//     */
//    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @GetMapping("/report/{reportId}")
    @Operation(
            summary = "Fetch report details",
            description = "Provides a full snapshot of a report for audit evaluation."
    )
    public ResponseEntity<GrantReportResponseDTO> getReportDetails(@PathVariable UUID reportId) {
        GrantReportResponseDTO detail = complianceService.getGrantReportSummary(reportId);
        return ResponseEntity.ok(detail);
    }
//
//    /**
//     * Checks whether an application is compliant for further disbursement.
//     *
//     * @param applicationId Application identifier.
//     * @return True if compliant, false otherwise.
//     */
//    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','FINANCE_OFFICER', 'ADMIN')")
    @GetMapping("/compliance-check/{applicationId}")
    @Operation(
            summary = "Check compliance readiness",
            description = "Used by Finance Officers to verify if an application is eligible for next disbursement."
    )
    public ResponseEntity<Boolean> getGrantReportStatus(@PathVariable UUID applicationId) {
        log.info("Compliance check for ApplicationID: {}", applicationId);

        boolean isCompliant = complianceService.isApplicantCompliant(applicationId);

        log.info("Compliance result | ApplicationID: {} | Status: {}", applicationId, isCompliant);
        return ResponseEntity.ok(isCompliant);
    }
}