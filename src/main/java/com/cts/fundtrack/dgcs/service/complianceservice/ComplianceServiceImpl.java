package com.cts.fundtrack.dgcs.service.complianceservice;

import com.cts.fundtrack.dgcs.client.applicationclient.ApplicationClient;
import com.cts.fundtrack.dgcs.client.dto.ApplicationMetadataDTO;

import com.cts.fundtrack.dgcs.dto.compliancedto.ApplicantComplianceDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckRequestDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckResponseDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceHistoryDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;

import com.cts.fundtrack.dgcs.exception.*;
import com.cts.fundtrack.dgcs.model.ComplianceCheck;
import com.cts.fundtrack.dgcs.model.GrantReport;
import com.cts.fundtrack.dgcs.model.enums.ComplianceStatus;
import com.cts.fundtrack.dgcs.model.enums.DisbursementStatus;
import com.cts.fundtrack.dgcs.model.enums.GrantReportStatus;

import com.cts.fundtrack.dgcs.repository.compliancecheckrepository.ComplianceCheckRepository;
import com.cts.fundtrack.dgcs.repository.disbursementrepository.DisbursementRepository;
import com.cts.fundtrack.dgcs.repository.grantreportrepository.GrantReportRepository;

import com.cts.fundtrack.dgcs.service.validation.ComplianceValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Enterprise-grade implementation of the {@link ComplianceService} interface.
 * <p>
 * This implementation serves as the central orchestration point for grant oversight.
 * It manages the lifecycle of compliance audits, ensuring that every review is
 * permanently recorded and that corresponding grant reports transition through
 * valid business states.
 * </p>
 * <p>
 * Core design principles:
 * <ul>
 * <li><b>Immutability of Terminal States:</b> Once a report is finalized, no further audits are permitted.</li>
 * <li><b>Transactional Integrity:</b> Audit logs and status updates are synchronized via Spring ACID transactions.</li>
 * <li><b>Data Transformation:</b> Entities are sanitized into DTOs for secure administrative visibility.</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceCheckRepository complianceCheckRepository;
    private final GrantReportRepository grantReportRepository;
    private final DisbursementRepository disbursementRepository;
    private final ApplicationClient applicationClient;
    private final ComplianceValidator complianceValidator;

    /**
     * {@inheritDoc}
     * <p>
     * This method implements a strict audit workflow:
     * <ol>
     * <li>Retrieves the report and verifies its current status.</li>
     * <li>Enforces a "one-time review" policy by rejecting terminal statuses (COMPLETED/FAILED).</li>
     * <li>Persists a {@link ComplianceCheck} record as a permanent audit trail.</li>
     * <li>Triggers a state transition on the {@link GrantReport} based on the audit outcome.</li>
     * </ol>
     * </p>
     *
     * @throws ProgramLifecycleException if an audit attempt is made on a finalized report.
     * @throws ResourceNotFoundException if the specified report ID is not present in the system.
     */
    @Override
    @Transactional
    public ComplianceCheckResponseDTO recordAudit(ComplianceCheckRequestDTO dto) {
        log.info("Audit Workflow Initiated | Report ID: {} | Verdict: {}", dto.getGrantReportId(), dto.getStatus());

        // 1. Fetch the report from the database
        GrantReport report = grantReportRepository.findById(dto.getGrantReportId())
                .orElseThrow(() -> {
                    log.error("Audit Aborted: Resource resolution failed for Report ID: {}", dto.getGrantReportId());
                    return new GrantReportNotFoundException("Report not found with ID: " + dto.getGrantReportId());
                });

        // 2. Ensure the report hasn't been audited already
        if (report.getStatus() == GrantReportStatus.APPROVED || report.getStatus() == GrantReportStatus.REJECTED) {
            log.warn("Integrity Violation: Attempted re-audit of terminal state | Report ID: {} | Current Status: {}",
                    report.getGrantReportId(), report.getStatus());
            throw new ReportLockedException("Audit Denied: This report has already been reviewed.");
        }

        // 3. Convert the input status into a system-readable format
        ComplianceStatus auditResult;
        try {
            auditResult = ComplianceStatus.valueOf(dto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid status provided: " + dto.getStatus());
        }

        // 4. Create and save the new audit record
        ComplianceCheck check = ComplianceCheck.builder()
                .grantReportId(report.getGrantReportId())
                .applicationId(report.getApplicationId())
                .complianceOfficerId(dto.getComplianceOfficerId())
                .type(dto.getType())
                .result(auditResult)
                .notes(dto.getComments())
                .date(Instant.now())
                .build();

        complianceCheckRepository.save(check);
        log.debug("Audit Record Persisted: Check ID {} for Application ID {}", check.getCheckId(), report.getApplicationId());

        // 5. Update the report's status based on the audit result
        GrantReportStatus previousStatus = report.getStatus();

        if (auditResult == ComplianceStatus.COMPLIANCE) {
            report.setStatus(GrantReportStatus.APPROVED);
        } else if (auditResult == ComplianceStatus.NON_COMPLIANT) {
            report.setStatus(GrantReportStatus.REJECTED);
        }

        grantReportRepository.save(report);

        log.info("Audit Workflow Finalized | Report ID: {} | Transition: {} -> {} ",
                report.getGrantReportId(), previousStatus, report.getStatus());

        // 6. Build and return the final summary to the caller
        return ComplianceCheckResponseDTO.builder()
                .checkId(check.getCheckId())
                .grantReportId(check.getGrantReportId())
                .status(check.getResult())
                .reportStatus(report.getStatus())
                .auditDate(check.getDate())
                .remarks(check.getNotes())
                .build();
    }

    /**
     * Retrieves a complete history of all audits performed by a specific compliance officer.
     * <p>
     * This method provides a chronological view of past decisions (APPROVED/REJECTED),
     * acting as the primary source for regulatory reporting and internal tracking.
     * </p>
     *
     * @param complianceOfficerId The unique identifier of the officer whose history is being requested.
     * @return A list of historical audit records, or an empty list if no audits have been performed.
     * @throws DataExportException If a system error prevents the retrieval of the audit records.
     */

    @Override
    @Transactional(readOnly = true)
    public List<ComplianceHistoryDTO> getComplianceHistoryByOfficer(UUID complianceOfficerId) {

        log.info("Compliance Audit Retrieval | Target Officer ID: {}", complianceOfficerId);

        try {
            // 1. Fetch all audit records linked to this officer
            List<ComplianceCheck> checks = complianceCheckRepository.findByComplianceOfficerId(complianceOfficerId);

            if (checks.isEmpty()) {
                log.info("Audit History Search | Result: No history found for Officer: {}", complianceOfficerId);
                return List.of();
            }

            // 2. Transform the database records into a readable history format
            List<ComplianceHistoryDTO> history = checks.stream()
                    .map(this::mapToHistoryDTO)
                    .toList();

            log.info("Audit History Search | Completed: Found {} records for Officer: {}",
                    history.size(), complianceOfficerId);

            return history;

        } catch (Exception e) {
            log.error("Audit History Search | Failed for Officer ID: {}", complianceOfficerId, e);
            throw new DataExportException("Could not retrieve audit history. Please try again later.");
        }
    }

    /**
     * Retrieves a detailed summary of a grant report for the compliance officer to review.
     * <p>
     * This method prepares all the necessary data for the audit screen, ensuring that
     * internal database records are converted into a clean, safe format for the user interface.
     * </p>
     *
     * @param reportId The unique ID of the report being reviewed.
     * @return A summarized view of the report including its status and attached documents.
     * @throws GrantReportNotFoundException If the report ID does not exist in our records.
     * @throws ComplianceDataException If the system encounters an error while organizing the report data.
     */
    @Override
    @Transactional(readOnly = true)
    public GrantReportResponseDTO getGrantReportSummary(UUID reportId) {
        // 1. Log the start of the retrieval process for tracking purposes
        log.info("Report Lookup | Starting search for Report ID: {}", reportId);

        return grantReportRepository.findById(reportId)
                .map(report -> {
                    try {
                        log.debug("Data Mapping | Projecting Entity [{}] to Response DTO.", report.getGrantReportId());
                        // 2. Build the summary and ensure the status is never blank
                        return GrantReportResponseDTO.builder()
                                .grantReportId(report.getGrantReportId())
                                .applicationId(report.getApplicationId())
                                .scope(report.getScope())
                                .metrics(report.getMetrics())
                                .status(report.getStatus() != null ? report.getStatus().name() : "PENDING")
                                .documentUrl(report.getProofPath())
                                .acknowledgment("Administrative report summary resolved successfully.")
                                .build();
                    } catch (Exception e) {
                        // 3. Handle any unexpected errors during data conversion
                        log.error("Data Mapping Error | Failed to process Report ID: {}", report.getGrantReportId());
                        throw new ComplianceDataException("The system could not process the report data for ID: " + report.getGrantReportId());
                    }

                })
                .orElseThrow(() -> {
                    // 4. Log and throw an error if the report is missing
                    log.warn("Report Lookup Failed | Report ID {} was not found.", reportId);
                    return new GrantReportNotFoundException("Could not find a report with ID: " + reportId);
                });
    }

    /**
     * Identifies applicants who have received funding but have not yet submitted their required progress reports.
     * <p>
     * <b>Logic:</b> An applicant is flagged as "delinquent" if the number of payments they have received
     * is greater than the number of reports they have submitted.
     * </p>
     *
     * @param programId The ID of the funding program to check.
     * @return A list of applicants who are currently behind on their reporting requirements.
     * @throws ProgramNotFoundException If the program ID provided is not valid.
     * @throws ComplianceViolationException If there is a system error during the check.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ApplicantComplianceDTO> getNonSubmittingApplicants(UUID programId) {
        log.info("Compliance Scan Initiated | Target Program: {}", programId);

        // 1. Find all applications in this program that have received payments
        List<UUID> applicationIds = disbursementRepository.findDistinctApplicationIdsByProgramId(programId);

        log.info("Delinquency Scan | Checking {} applications for missing reports.", applicationIds.size());

        if (applicationIds.isEmpty()) {
            return List.of();
        }

        try {
            return applicationIds.stream()
                    .filter(appId -> {
                        // 2. Identify delinquency: Has the applicant been paid more times than they've reported?
                        long paidCount = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);
                        long reportCount = grantReportRepository.countByApplicationId(appId);
                        return paidCount > reportCount;
                    })
                    .map(appId -> {
                        // 3. Build the compliance record for flagged applicants
                        long paidInstallments = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);
                        //ApplicationMetadataDTO metadata = applicationClient.getApplicationMetadata(appId);
                        // Note: In a real microservice, you'd call 'ApplicationService'
                        // here to get names. For now, we use placeholders or IDs.
                        return ApplicantComplianceDTO.builder()
                                .applicationId(appId)
                                .applicantName("metadata.getApplicantName()")
                                .programName("metadata.getProgramName()")
                                .applicationStatus("metadata.getStatus()")
                                .latestReportStatus("DELINQUENT_SUBMISSION_REQUIRED")
                                .currentInstallment((int) paidInstallments)
                                .build();
                    })
                    .toList();

        } catch (Exception e) {
            // 4. Handle unexpected system failures during the scan
            log.error("Delinquency Scan Failed | Critical error during scan: {}", e.getMessage());
            throw new ComplianceViolationException("The system could not complete the delinquency check.");
        }
    }

    /**
     * Checks if an application is currently eligible for further actions (like payments).
     * <p>
     * This method acts as a safety check to ensure that the applicant is following
     * all rules. Specifically, they must have at least one submitted report, and
     * their most recent report must be 'APPROVED'.
     * </p>
     *
     * @param applicationId The unique ID of the application to check.
     * @return true if the applicant has met all rules; false if they are missing reports or were rejected.
     * @throws ApplicationNotFoundException If the application ID does not exist.
     * @throws ComplianceViolationException If the system's rule engine fails to run.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isApplicantCompliant(UUID applicationId) {
        // 1. Log the start of the compliance check
        log.info("Compliance Check | Verifying status for Application: {}", applicationId);
        try {
            // 2. Ask the validator to check all business rules for this ID
            boolean isCompliant = complianceValidator.verifyCompliance(applicationId);

            log.info("Compliance Validation Finalized | AppID: {} | Outcome: {}",
                    applicationId, isCompliant ? "COMPLIANT" : "NON_COMPLIANT");

            return isCompliant;

        } catch (Exception e) {
            // 3. Handle errors if the rule engine breaks
            log.error("Compliance Check | System Error for Application: {}. Details: {}",
                    applicationId, e.getMessage());

            throw new ComplianceViolationException("The system could not determine compliance status at this time.");
        }
    }

    /**
     * Generates a complete overview of all applicants' compliance status within a specific program.
     * <p>
     * This method powers the Admin Dashboard. It compares how many payments an applicant
     * has received against their report history to determine if they are "Up to Date"
     * or "Behind" on their requirements.
     * </p>
     *
     * @param programId The ID of the funding program to summarize.
     * @return A list of compliance summaries for every applicant in the program.
     * @throws ComplianceDataException If the system fails to gather the summary data.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ApplicantComplianceDTO> getGrantReportForProgram(UUID programId) {
        log.info("Administrative Audit | Initiating summary for Program: {}", programId);

        // 1. Find all application IDs that have financial activity in this program
        List<UUID> applicationIds = disbursementRepository.findDistinctApplicationIdsByProgramId(programId);

        if (applicationIds.isEmpty()) {
            log.warn("Audit Scan | No active applications found for Program: {}", programId);
            return List.of();
        }

        try {
            log.info("Dashboard Update | Processing {} applications.", applicationIds.size());

            return applicationIds.stream().map(appId -> {
                try {
                    // 2. Gather metrics: count payments and fetch the report history
                    long paidInstallments = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);

                    List<GrantReport> reports = grantReportRepository.findByApplicationIdOrderBySubmittedDateDesc(appId);

                    // 3. Determine the current compliance standing
                    String complianceStatus = resolveStatus(reports, paidInstallments);

                    // 4. Build the summary record for this specific application
                   // ApplicationMetadataDTO metadata = applicationClient.getApplicationMetadata(appId);
                    return ApplicantComplianceDTO.builder()
                            .applicationId(appId)
                            .applicantName("metadata.getApplicantName()") // Placeholder for Feign/Rest call
                            .programName("metadata.getProgramName()")
                            .applicationStatus("metadata.getStatus()")
                            .latestReportStatus(complianceStatus)
                            .currentInstallment((int) paidInstallments)
                            .build();

                } catch (Exception rowError) {
                    log.error("Dashboard Update | Skipping App ID {} due to error: {}", appId, rowError.getMessage());
                    return ApplicantComplianceDTO.builder()
                            .applicationId(appId)
                            .latestReportStatus("METADATA_CORRUPTION_ERROR")
                            .build();
                }
            }).toList();

        } catch (Exception globalError) {
            // 6. Handle a total system failure
            log.error("Dashboard Update | Critical failure for Program {}: {}", programId, globalError.getMessage());
            throw new ComplianceDataException("The system could not generate the dashboard summary.");
        }
    }

    /**
     * Calculates the current standing of an application by comparing payments to reports.
     * <p>
     * This method acts as a "Gap Checker." It looks at how many times we've paid the applicant
     * versus how many reports they've sent back to determine if they are eligible for more
     * funding or if they are currently delinquent.
     * </p>
     *
     * @param reports          A list of all reports submitted, with the newest ones first.
     * @param paidInstallments The total number of payments already sent to the applicant.
     * @return A clear status message (e.g., "READY", "MISSING_REPORT", or the latest report status).
     */
    private String resolveStatus(List<GrantReport> reports, long paidInstallments) {
        // 1. Check for a "Brand New" project with no activity yet
        if (reports.isEmpty() && paidInstallments == 0) {
            log.debug("Status Check | Project is new. Result: READY_FOR_FIRST_DISBURSEMENT");
            return "READY_FOR_FIRST_DISBURSEMENT";
        }

        // 2. Check if the applicant is behind on their reporting
        // If they've been paid more times than they've reported, they are delinquent.
        if (reports.size() < paidInstallments) {
            String missingStatus = "MISSING_REPORT_FOR_INSTALLMENT_" + (reports.size() + 1);
            log.warn("Compliance Gap | App requires report for next phase. Current Reports: {} | Paid Phases: {}",
                    reports.size(), paidInstallments);
            return missingStatus;
        }

        // 3. Return the status of the most recent report they submitted
        return reports.stream()
                .findFirst()
                .map(report -> {
                    log.debug("Status Check | Active project. Latest status: {}", report.getStatus());
                    return report.getStatus().name();
                })
                .orElseGet(() -> {
                    log.info("Status Check | No reports found in system.");
                    return "NO_REPORTS_SUBMITTED";
                });
    }

    /**
     * Internal utility to transform a {@link ComplianceCheck} domain entity into
     * a sanitized {@link ComplianceHistoryDTO}.
     *
     * @param check The source persistence entity.
     * @return A flattened DTO suitable for historical reporting.
     */
    private ComplianceHistoryDTO mapToHistoryDTO(ComplianceCheck check) {
        return ComplianceHistoryDTO.builder()
                .checkId(check.getCheckId())
                .grantReportId(check.getGrantReportId())
                .applicationId(check.getApplicationId())
                .complianceOfficerId(check.getComplianceOfficerId())
                .auditType(check.getType())
                .result(check.getResult().name())
                .notes(check.getNotes())
                .auditDate(check.getDate().toString())
                .build();
    }
}
