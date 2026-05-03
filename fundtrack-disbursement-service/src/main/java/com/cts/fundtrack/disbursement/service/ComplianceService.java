package com.cts.fundtrack.disbursement.service;


import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.ApplicantComplianceDTO;
import com.cts.fundtrack.common.dto.ComplianceCheckRequestDTO;
import com.cts.fundtrack.common.dto.ComplianceHistoryDTO;
import com.cts.fundtrack.common.dto.GrantReportResponseDTO;

/**
 * Service interface defining the governance and audit workflows for grant compliance.
 * <p>
 * This service provides the essential logic for Compliance Officers to review
 * submitted reports, maintain a permanent audit trail, and monitor the
 * health of various grant programs via specialized dashboards.
 * </p>
 */
public interface ComplianceService {

    /**
     * Executes and persists a formal audit review for a submitted grant report.
     * <p>
     * This operation validates the report's current state and transitions it to
     * a terminal status (APPROVED or FAILED) based on the officer's evaluation.
     * </p>
     *
     * @param dto The {@link ComplianceCheckRequestDTO} containing the evaluation outcome and comments.
     * @return A status message confirming the record creation and state transition.
     * @throws// com.cts.fundtrack.exception.ProgramLifecycleException if the report is in an invalid state for auditing.
     */
    String recordAudit(ComplianceCheckRequestDTO dto);

    /**
     * Aggregates real-time compliance metrics for all applications within a program.
     * <p>
     * Primarily used for dashboard visualization, this method correlates applications
     * with their most recent reporting activity to identify potential bottlenecks.
     * </p>
     *
     * @param programId The unique identifier of the grant program.
     * @return A collection of {@link ApplicantComplianceDTO} objects for administrative review.
     */
    List<ApplicantComplianceDTO> getApplicantGrantReportingSummary(UUID programId);

    /**
     * Retrieves the comprehensive immutable history of all compliance audits performed.
     * <p>
     * Facilitates regulatory reporting and internal accountability by providing
     * a detailed log of every manual and automated compliance check.
     * </p>
     *
     * @return A list of {@link ComplianceHistoryDTO} representing the global audit trail.
     */
    List<ComplianceHistoryDTO> getComplianceHistoryByOfficer(UUID complianceOfficerId);

//    /**
//     * Fetches the compliance-centric details for a specific reporting resource.
//     * <p>
//     * Resolves the entity from the persistence layer and maps it to a UI-friendly
//     * DTO format containing applicant and program context.
//     * </p>
//     *
//     * @param reportId The unique identifier of the target report.
//     * @return A summarized {@link ApplicantComplianceDTO} of the report's current state.
//     * @throws RuntimeException if the resource cannot be located.
//     */
        GrantReportResponseDTO getGrantReportSummary(UUID reportId);
//
//    /**
//     * Identifies applicants who have received payments but have not yet
//     * submitted the required grant reports to justify those funds.
//     * <p>
//     * This acts as a "Delinquency Filter," identifying cases where the disbursement
//     * count exceeds the submitted report count.
//     * </p>
//     * * @param programId The unique identifier of the grant program to audit.
//     * @return A list of applicants marked with a "MISSING_REPORT" status.
//     */
    List<ApplicantComplianceDTO> getNonSubmittingApplicants(UUID programId);
//
//
    /**
     * Verifies if the required compliance documentation has been compiled for an application.
     * <p>
     * This method is primarily utilized by Finance Officers to determine if the
     * reporting obligations for the current disbursement cycle have been satisfied.
     * </p>
     *
     * @param applicationId The unique identifier of the application to check.
     * @return {@code true} if the compliance report is finalized; {@code false} otherwise.
     */
    boolean isApplicantCompliant(UUID applicationId);

}