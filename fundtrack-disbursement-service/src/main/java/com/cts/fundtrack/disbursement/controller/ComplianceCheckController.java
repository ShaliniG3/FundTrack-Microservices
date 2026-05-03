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

/**
 * REST controller exposing compliance audit and governance endpoints for the FundTrack platform.
 * <p>
 * This controller enables Compliance Officers and Administrators to manage the full audit
 * lifecycle of submitted grant reports. It provides endpoints for recording audit verdicts,
 * monitoring program-level reporting health via a dashboard, retrieving historical audit
 * trails, identifying delinquent applicants, and performing pre-disbursement compliance checks.
 * </p>
 * <p>
 * All endpoints are reachable under {@code /api/v1/compliance} and are restricted to
 * users with the {@code COMPLIANCE_OFFICER} or {@code ADMIN} role, unless otherwise noted.
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
     * Records a formal audit verdict (approval or rejection) for a submitted grant report.
     * <p>
     * The officer's decision is persisted as an immutable {@code ComplianceCheck} record,
     * and the associated grant report's status is transitioned to a terminal state
     * ({@code APPROVED} or {@code REJECTED}). Reports already in a terminal state
     * cannot be re-audited. Restricted to Compliance Officers and Admins.
     * </p>
     *
     * @param dto the {@link ComplianceCheckRequestDTO} containing the grant report ID,
     *            the officer's UUID, the verification type, the status decision, and any comments
     * @return a {@link ResponseEntity} with a confirmation message describing the final
     *         report status after the audit
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
     * Retrieves an aggregated compliance dashboard for all applicants within a program.
     * <p>
     * For each approved application in the specified program, this endpoint computes
     * the current compliance status by correlating paid disbursement installments with
     * submitted grant reports. Useful for identifying bottlenecks at a program level.
     * Restricted to Compliance Officers and Admins.
     * </p>
     *
     * @param programId the UUID of the grant program whose compliance summary is requested
     * @return a {@link ResponseEntity} containing a list of {@link ApplicantComplianceDTO}
     *         objects, each representing one applicant's current reporting status
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
     * Retrieves the complete audit history performed by a specific compliance officer.
     * <p>
     * Returns all {@code ComplianceCheck} records associated with the given officer UUID,
     * providing a traceable log of every audit decision they have made. Restricted to
     * Compliance Officers and Admins.
     * </p>
     *
     * @param complianceOfficerId the UUID of the compliance officer whose history is requested
     * @return a {@link ResponseEntity} containing a list of {@link ComplianceHistoryDTO}
     *         objects representing each audit record performed by that officer
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
     * Identifies applicants who have received disbursements but have not submitted
     * the corresponding grant progress reports.
     * <p>
     * Applies a delinquency filter: any application where the count of paid disbursement
     * installments exceeds the count of submitted reports is flagged. Accessible by
     * Compliance Officers, Finance Officers, and Admins.
     * </p>
     *
     * @param programId the UUID of the grant program to scan for non-submitters
     * @return a {@link ResponseEntity} containing a list of {@link ApplicantComplianceDTO}
     *         objects for applications with a {@code DELINQUENT_SUBMISSION_REQUIRED} status
     */
    @GetMapping("/non-submitters/{programId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'FINANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Identify applicants with missing submissions")
    public ResponseEntity<List<ApplicantComplianceDTO>> getNonSubmitters(@PathVariable UUID programId) {
        log.info("Fetching non-submitter list for ProgramID: {}", programId);
        List<ApplicantComplianceDTO> list = complianceService.getNonSubmittingApplicants(programId);
        return ResponseEntity.ok(list);
    }

    /**
     * Fetches the full details of a specific grant report for compliance review.
     * <p>
     * Resolves the grant report entity by its UUID and returns a summarized DTO
     * containing scope, metrics, current status, and the document URL for the
     * uploaded proof of fund utilization. Restricted to Compliance Officers and Admins.
     * </p>
     *
     * @param reportId the UUID of the grant report to retrieve
     * @return a {@link ResponseEntity} containing the {@link GrantReportResponseDTO}
     *         with the report's details
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
     * Performs a real-time compliance readiness check for an application before disbursement.
     * <p>
     * This gate-check verifies that the applicant has fulfilled all reporting obligations
     * for previously received installments (i.e., each paid disbursement has a corresponding
     * approved grant report). Finance Officers use this before authorising the next payment.
     * Accessible by Compliance Officers, Finance Officers, and Admins.
     * </p>
     *
     * @param applicationId the UUID of the grant application to verify
     * @return a {@link ResponseEntity} containing {@code true} if the applicant is compliant
     *         and eligible for the next disbursement, or {@code false} otherwise
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