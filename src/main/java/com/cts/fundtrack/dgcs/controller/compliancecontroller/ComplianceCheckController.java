package com.cts.fundtrack.dgcs.controller.compliancecontroller;

import com.cts.fundtrack.dgcs.dto.compliancedto.ApplicantComplianceDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckRequestDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckResponseDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceHistoryDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;

import com.cts.fundtrack.dgcs.service.complianceservice.ComplianceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.springframework.security.access.prepost.PreAuthorize;

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
     * Records a formal audit verdict (approval or rejection) for a submitted grant report.
     * <p>
     * This operation is a critical lifecycle event. A 'COMPLIANT' status typically
     * triggers the release of the next scheduled disbursement, while 'NON_COMPLIANT'
     * halts the funding process until the applicant rectifies the issues.
     * </p>
     *
     * @param dto Audit verdict details including report ID, compliance status, and officer remarks.
     * @return A {@link ResponseEntity} containing the updated audit record details.
     */
    @Operation(
            summary = "Record Audit Verdict",
            description = "Allows Compliance Officers to submit an approval or rejection decision. "
                    + "Submitting a COMPLIANT verdict may automatically unlock the next payment installment.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Audit verdict successfully recorded",
                            content = @Content(schema = @Schema(implementation = ComplianceCheckResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Validation failed or report state mismatch"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient compliance privileges"),
                    @ApiResponse(responseCode = "404", description = "Not Found: The specified Report ID does not exist")
            }
    )
    //@PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @PostMapping("/audit")
    public ResponseEntity<ComplianceCheckResponseDTO> recordAudit(@Valid @RequestBody ComplianceCheckRequestDTO dto) {

        log.info("Ingress Request | POST /api/v1/compliance/audit | ReportID: {} | Status: {}",
                dto.getGrantReportId(), dto.getStatus());

        ComplianceCheckResponseDTO response = complianceService.recordAudit(dto);

        if (response == null) {
            log.error("Egress Response | Audit failed to generate a response object for ReportID: {}", dto.getGrantReportId());
            return ResponseEntity.internalServerError().build();
        }

        log.info("Egress Response | Audit Finalized | ReportID: {} | Verdict: {}",
                dto.getGrantReportId(), response.getStatus());

        return ResponseEntity.ok(response);
    }

    /**
     * Fetches the complete audit history associated with a specific Compliance Officer.
     * <p>
     * This endpoint allows administrators and senior compliance staff to review
     * the decision history of an individual officer. It provides a chronological
     * view of all approvals, rejections, and remarks issued by the specified user.
     * </p>
     *
     * @param complianceOfficerId The unique UUID of the officer whose history is being retrieved.
     * @return A {@link ResponseEntity} containing a list of audit records.
     */
    @Operation(
            summary = "Retrieve Officer Audit History",
            description = "Returns all compliance actions performed by a specific officer. "
                    + "Used for internal quality control and accountability tracking. "
                    + "Restricted to COMPLIANCE_OFFICER and ADMIN roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Audit history retrieved successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad Request: Invalid Officer UUID format"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient privileges to view audit trails"),
                    @ApiResponse(responseCode = "404", description = "Not Found: No history found for the provided Officer ID")
            }
    )
    // @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @GetMapping("/compliance_history")
    public ResponseEntity<List<ComplianceHistoryDTO>> getComplianceHistoryByOfficer(
            @Parameter(description = "The unique UUID of the compliance officer", required = true, example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
            @RequestParam("complianceOfficerId") UUID complianceOfficerId) {

        log.info("REST Request: Fetching compliance history for Officer ID: {}", complianceOfficerId);

        List<ComplianceHistoryDTO> history = complianceService.getComplianceHistoryByOfficer(complianceOfficerId);
        List<ComplianceHistoryDTO> safeHistory = (history != null) ? history : List.of();
        log.info("REST Response: Found {} records for Officer ID: {}", safeHistory.size(), complianceOfficerId);

        return ResponseEntity.ok(safeHistory);
    }

    /**
     * Retrieves comprehensive details for a specific grant report to facilitate audit review.
     * <p>
     * This endpoint provides a read-only snapshot of the applicant's submission, including
     * metadata and links to supporting evidence. It serves as the primary data source
     * for Compliance Officers during the evaluation workflow.
     * </p>
     *
     * @param reportId The unique identifier of the grant report to be audited.
     * @return A {@link ResponseEntity} containing the detailed report summary.
     */
    @Operation(
            summary = "Fetch Report Details for Audit",
            description = "Provides a full snapshot of a report's data and evidence. "
                    + "Designed for Compliance Officers to perform manual evaluations. "
                    + "Restricted to COMPLIANCE_OFFICER and ADMIN roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report details successfully retrieved"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User lacks permission to view this report"),
                    @ApiResponse(responseCode = "404", description = "Not Found: No report exists with the provided ID"),
                    @ApiResponse(responseCode = "500", description = "Internal Error: Failure to compile report metadata")
            }
    )
    //    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @GetMapping("/grant_report/{reportId}")
    public ResponseEntity<GrantReportResponseDTO> getGrantReportSummary(@Parameter(description = "The unique UUID of the grant report", example = "7b23-4e89-9a12-c56789012345")
                                                                        @PathVariable UUID reportId) {
        log.info("Ingress Request | GET /api/v1/compliance/report/{} | ReportID: {}", reportId, reportId);

        GrantReportResponseDTO detail = complianceService.getGrantReportSummary(reportId);

        if (detail == null) {
            log.warn("Egress Response | Report Not Found | ReportID: {}", reportId);
            return ResponseEntity.notFound().build();
        }

        log.info("Egress Response | Report Details Retrieved | ReportID: {} | Status: {}",
                reportId, detail.getStatus());

        return ResponseEntity.ok(detail);
    }

    /**
     * Retrieves the comprehensive compliance dashboard for an entire grant program.
     * <p>
     * This administrative view aggregates the compliance status of all associated
     * applications. It allows officers to identify "at-risk" applicants, track
     * submission rates, and prioritize pending audits for a specific program cycle.
     * </p>
     *
     * @param programId The unique identifier of the grant program.
     * @return A {@link ResponseEntity} containing a list of applicant compliance summaries.
     */
    @Operation(
            summary = "Get Program Compliance Dashboard",
            description = "Returns a consolidated list of compliance summaries for all applications. "
                    + "Used for high-level monitoring and Identifying non-compliant participants. "
                    + "Restricted to COMPLIANCE_OFFICER and ADMIN roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dashboard data compiled successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: User lacks administrative monitoring privileges"),
                    @ApiResponse(responseCode = "404", description = "Not Found: No program exists with the provided ID"),
                    @ApiResponse(responseCode = "500", description = "Internal Error: Aggregation logic failed")
            }
    )
    // @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @GetMapping("/dashboard/program/{programId}")
    public ResponseEntity<List<ApplicantComplianceDTO>> getGrantReportForProgram(
            @Parameter(description = "The unique UUID of the grant program", example = "3c2a-9e11-4f88-b234-556677889900")
            @PathVariable UUID programId) {

        log.info("Ingress Request | GET /api/v1/compliance/dashboard/program/{} | ProgramID: {}", programId, programId);

        List<ApplicantComplianceDTO> dashboard = complianceService.getGrantReportForProgram(programId);

        List<ApplicantComplianceDTO> safeDashboard = (dashboard != null) ? dashboard : List.of();

        log.info("Egress Response | Dashboard Compiled | ProgramID: {} | Total Applications: {}",
                programId, safeDashboard.size());
        return ResponseEntity.ok(safeDashboard);
    }

    /**
     * Identifies applicants within a specific program who have missed their reporting deadlines.
     * <p>
     * This utility endpoint cross-references scheduled disbursement milestones against
     * actual report submissions. It generates a "strike list" of applicants who are
     * currently overdue, allowing Compliance Officers to send reminders or pause
     * future payments.
     * </p>
     *
     * @param programId The unique identifier of the grant program.
     * @return A {@link ResponseEntity} containing a list of applicants marked as non‑submitters.
     */
    @Operation(
            summary = "Identify Missing Submissions",
            description = "Lists all applicants who have failed to submit required progress reports. "
                    + "Used for enforcement actions and automated reminders. "
                    + "Restricted to COMPLIANCE, FINANCE, and ADMIN roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Non-submitter list compiled successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient privileges to view enforcement lists"),
                    @ApiResponse(responseCode = "404", description = "Not Found: Program ID does not exist"),
                    @ApiResponse(responseCode = "500", description = "Internal Error: Deadline calculation engine failure")
            }
    )
    //    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','FINANCE_OFFICER', 'ADMIN')")
    @GetMapping("/non-submitters/{programId}")
    public ResponseEntity<List<ApplicantComplianceDTO>> getNonSubmitters(
            @Parameter(description = "The unique UUID of the grant program", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
            @PathVariable UUID programId) {

        log.info("Ingress Request | GET /api/v1/compliance/non-submitters/{} | ProgramID: {}", programId, programId);

        // Fetching filtered 'overdue' list from service layer
        List<ApplicantComplianceDTO> list = complianceService.getNonSubmittingApplicants(programId);

        // Defensive Null-Safety: Ensuring the API returns [] instead of null
        List<ApplicantComplianceDTO> safeList = (list != null) ? list : List.of();

        log.info("Egress Response | Non-Submitter Audit Complete | ProgramID: {} | Overdue Count: {}",
                programId, safeList.size());

        return ResponseEntity.ok(safeList);
    }

    /**
     * Verifies the compliance eligibility of an application for subsequent fund disbursement.
     * <p>
     * This high-frequency check is primarily utilized by the Finance module's automated
     * payment engine. It validates that the applicant has satisfied all reporting
     * obligations and has received positive audit verdicts for all previous milestones.
     * </p>
     *
     * @param applicationId The unique identifier of the grant application.
     * @return A {@link ResponseEntity} containing true if all compliance gates are passed, false otherwise.
     */
    @Operation(
            summary = "Check Compliance Readiness",
            description = "Determines if an application is eligible for its next payment installment. "
                    + "Evaluates against missing reports and 'NON_COMPLIANT' verdicts. "
                    + "Typically called by the Finance engine before finalizing payments.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Compliance check completed successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient privileges to perform eligibility checks"),
                    @ApiResponse(responseCode = "404", description = "Not Found: Application ID does not exist"),
                    @ApiResponse(responseCode = "500", description = "Internal Error: Compliance engine timeout or failure")
            }
    )
    //    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','FINANCE_OFFICER', 'ADMIN')")
    @GetMapping("/compliance-check/{applicationId}")
    public ResponseEntity<Boolean> isApplicantCompliant(
            @Parameter(description = "The unique UUID of the grant application", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
            @PathVariable UUID applicationId) {

        log.info("Ingress Request | GET /api/v1/compliance/compliance-check/{} | ApplicationID: {}", applicationId, applicationId);

        boolean isCompliant = complianceService.isApplicantCompliant(applicationId);

        log.info("Egress Response | Eligibility Check Complete | ApplicationID: {} | IsEligible: {}",
                applicationId, isCompliant);

        return ResponseEntity.ok(isCompliant);
    }
}