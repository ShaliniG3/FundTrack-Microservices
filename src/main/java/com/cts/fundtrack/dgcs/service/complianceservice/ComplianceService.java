package com.cts.fundtrack.dgcs.service.complianceservice;

import com.cts.fundtrack.dgcs.dto.compliancedto.ApplicantComplianceDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckRequestDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckResponseDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceHistoryDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the governance, audit, and regulatory oversight workflows.
 * <p>
 * This service acts as the primary engine for grant accountability. It enables
 * Compliance Officers to perform manual report evaluations, provides real-time
 * monitoring via administrative dashboards, and maintains an immutable audit trail
 * to ensure fiduciary integrity across all grant programs.
 * </p>
 */
public interface ComplianceService {

    /**
     * Executes and persists a formal audit verdict for a submitted grant report.
     * <p>
     * This operation marks the transition of a report from a 'PENDING' state to a
     * terminal 'APPROVED' or 'REJECTED' status. It captures the officer's remarks
     * and a digital timestamp, effectively creating a legal record of the decision.
     * </p>
     *
     * @param dto The {@link ComplianceCheckRequestDTO} containing the verdict and justification.
     * @return A {@link ComplianceCheckResponseDTO} confirming the audit result and updated report state.
     */
    ComplianceCheckResponseDTO recordAudit(ComplianceCheckRequestDTO dto);

    /**
     * Aggregates real-time compliance metrics for a comprehensive program dashboard.
     * <p>
     * Correlates multi-service data to provide a high-level view of all applications.
     * It highlights the current standing of each applicant, including their latest
     * report status and upcoming reporting milestones.
     * </p>
     *
     * @param programId The unique identifier of the grant program to monitor.
     * @return A list of {@link ApplicantComplianceDTO} objects for administrative visualization.
     */
    List<ApplicantComplianceDTO> getGrantReportForProgram(UUID programId);

    /**
     * Retrieves the complete immutable history of audits performed by a specific officer.
     * <p>
     * Facilitates internal quality control and regulatory transparency. By providing
     * a filtered view of audit events, it ensures accountability and supports
     * supervisor-level reviews of compliance decisions.
     * </p>
     *
     * @param complianceOfficerId The unique UUID of the officer whose history is being retrieved.
     * @return A list of {@link ComplianceHistoryDTO} representing the officer's decision trail.
     */
    List<ComplianceHistoryDTO> getComplianceHistoryByOfficer(UUID complianceOfficerId);

    /**
     * Retrieves detailed report metadata and evidence summaries for audit evaluation.
     * <p>
     * Acts as the primary data source for the "Investigation" phase of an audit,
     * providing officers with all the necessary context from the applicant's
     * submission to make an informed compliance decision.
     * </p>
     *
     * @param reportId The unique identifier of the target grant report.
     * @return A {@link GrantReportResponseDTO} containing the full report context.
     */
    GrantReportResponseDTO getGrantReportSummary(UUID reportId);

    /**
     * Identifies applicants who are currently in breach of their reporting obligations.
     * <p>
     * This acts as a "Delinquency Engine," cross-referencing disbursement dates
     * against missing submissions to flag "at-risk" applicants who have received
     * funds but failed to justify their usage.
     * </p>
     *
     * @param programId The unique identifier of the grant program to audit for missing reports.
     * @return A list of {@link ApplicantComplianceDTO}s representing non-submitting participants.
     */
    List<ApplicantComplianceDTO> getNonSubmittingApplicants(UUID programId);

    /**
     * Performs a critical verification check for the automated payment engine.
     * <p>
     * Primarily utilized by the Finance module, this method determines if an
     * application has satisfied all reporting requirements necessary to unlock
     * the next scheduled fund installment.
     * </p>
     *
     * @param applicationId The unique identifier of the application to verify.
     * @return {@code true} if all compliance gates are cleared; {@code false} if payments should be halted.
     */
    boolean isApplicantCompliant(UUID applicationId);
}