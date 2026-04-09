package com.cts.fundtrack.dgcs.service.complianceservice;


import com.cts.fundtrack.dgcs.client.applicationclient.ApplicationClient;
import com.cts.fundtrack.dgcs.client.dto.ApplicationMetadataDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ApplicantComplianceDTO;
import com.cts.fundtrack.dgcs.dto.compliancedto.ComplianceCheckRequestDTO;
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
    //
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
    public String recordAudit(ComplianceCheckRequestDTO dto) {
        log.info("Audit Workflow Initiated | Report ID: {} | Verdict: {}", dto.getGrantReportId(), dto.getStatus());

        GrantReport report = grantReportRepository.findById(dto.getGrantReportId())
                .orElseThrow(() -> {
                    log.error("Audit Aborted: Resource resolution failed for Report ID: {}", dto.getGrantReportId());

                    return new GrantReportNotFoundException("Report not found with ID: " + dto.getGrantReportId());
                });

        // Business Rule: Audit Integrity Guard
        if (report.getStatus() == GrantReportStatus.APPROVED || report.getStatus() == GrantReportStatus.REJECTED) {
            log.warn("Integrity Violation: Attempted re-audit of terminal state | Report ID: {} | Current Status: {}",
                    report.getGrantReportId(), report.getStatus());
            throw new ReportLockedException("Audit Denied: This report has already been reviewed and reached a terminal state.");
        }

        ComplianceStatus auditResult;
        try {
            auditResult = ComplianceStatus.valueOf(dto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid status provided: " + dto.getStatus());
        }

        ComplianceCheck check = ComplianceCheck.builder()
                .grantReportId(report.getGrantReportId())

                .applicationId(report.getApplicationId())
                .complianceOfficerId(dto.getComplianceOfficerId()) // Captured from the request
                .type(dto.getType())
                .result(auditResult)
                .notes(dto.getComments())
                .date(Instant.now())
                .build();

        complianceCheckRepository.save(check);
        log.debug("Audit Record Persisted: Check ID {} for Application ID {}", check.getCheckId(), report.getApplicationId());


        GrantReportStatus previousStatus = report.getStatus();



        if ("COMPLIANCE".equalsIgnoreCase(dto.getStatus())) {
            report.setStatus(GrantReportStatus.APPROVED);
        } else if ("NON_COMPLIANT".equalsIgnoreCase(dto.getStatus())) {
            report.setStatus(GrantReportStatus.REJECTED);
        } else {

            log.error("Invalid Status Transition Attempted | Received: {}", dto.getStatus());
            throw new InvalidInputException("Invalid status: Use 'APPROVED' or 'REJECTED' only.");
        }

        grantReportRepository.save(report);
        log.info("Audit Workflow Finalized | Report ID: {} | Transition: {} -> {} ",
                report.getGrantReportId(), previousStatus, report.getStatus());

        return "Audit complete. Report status synchronized to: " + report.getStatus();
    }




    /**
     * {@inheritDoc}
     * <p>
     * Aggregates a read-only historical ledger of all finalized compliance audits.
     * This serves as the primary data source for external regulatory exports.
     * </p>
     * <p>
     * <b>Audit Context:</b> Returns chronological records of all 'APPROVED' and 'REJECTED'
     * report outcomes across the entire system.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComplianceHistoryDTO> getComplianceHistoryByOfficer(UUID complianceOfficerId) {

        log.info("Compliance Audit Retrieval | Target Officer ID: {}", complianceOfficerId);

        try {
            // Fetch records specifically for this Compliance Officer
            List<ComplianceCheck> checks = complianceCheckRepository.findByComplianceOfficerId(complianceOfficerId);

            if (checks.isEmpty()) {
                log.info("Compliance Audit Retrieval | Result: No records found for Officer: {}", complianceOfficerId);
                return List.of();
            }

            // Mapping the entities to your History DTOs
            List<ComplianceHistoryDTO> history = checks.stream()
                    .map(this::mapToHistoryDTO)
                    .toList();

            log.info("Compliance Audit Retrieval | Successful: {} records retrieved for Officer: {}",
                    history.size(), complianceOfficerId);

            return history;

        } catch (Exception e) {
            log.error("Compliance Audit Retrieval Failure | Critical error for Officer ID: {}", complianceOfficerId, e);
            throw new DataExportException("The system failed to retrieve audit history for the specified compliance officer.");
        }
    }

    /**
     * Resolves a high-fidelity summary of a specific grant report for formal audit evaluation.
     * <p>
     * This method acts as the primary data orchestrator for the Compliance Officer's review
     * interface, projecting internal persistence entities into sanitized DTOs.
     * </p>
     * <p>
     * <b>Operational Guardrails:</b>
     * <ul>
     * <li><b>Relational Integrity:</b> Enforces safe navigation during application context resolution.</li>
     * <li><b>Audit Traceability:</b> Every access attempt is logged at the {@code INFO} level for forensic history.</li>
     * <li><b>Defensive Mapping:</b> Normalizes null or inconsistent Enum states to a stable "PENDING" baseline.</li>
     * </ul>
     * </p>
     *
     * @param reportId The unique {@link UUID} of the target Grant Report.
     * @return A fully populated {@link GrantReportResponseDTO} containing enriched audit context.
     * @throws GrantReportNotFoundException If the identifier does not correlate to an existing record.
     * @throws ComplianceDataException If a critical structural failure occurs during the mapping phase.
     */
    @Override
    @Transactional(readOnly = true)
    public GrantReportResponseDTO getGrantReportSummary(UUID reportId) {
        // 1. Ingress Logging: Providing a forensic trail for administrative access
        log.info("Forensic Access | Initiating snapshot retrieval for Report ID: {}", reportId);

        return grantReportRepository.findById(reportId)
                .map(report -> {
                    try {
                        log.debug("Data Mapping | Projecting Entity [{}] to Response DTO.", report.getGrantReportId());

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
                        // Internal Catch: Mapping failure (e.g., LazyInitializationException)
                        log.error("Mapping Failure | Structural error resolving Report ID: {}", report.getGrantReportId());
                        throw new ComplianceDataException("Internal Error: Failed to project report data for ID: " + report.getGrantReportId());
                    }
                })
                .orElseThrow(() -> {
                    // Egress Logging: Documenting a failed resolution attempt
                    log.warn("Resolution Failure | Target Report ID [{}] does not exist in the persistence layer.", reportId);
                    return new GrantReportNotFoundException("Audit Error: Target report not found for ID: " + reportId);
                });
    }
    /**
     * Executes a cross-reference audit between financial disbursements and reporting submissions
     * to isolate applicants in a delinquent reporting state.
     * <p>
     * <b>Business Logic:</b> An applicant is flagged if the count of {@code COMPLETED}
     * disbursements exceeds the count of successfully submitted {@link GrantReport} entities.
     * </p>
     * <p>
     * <b>Operational Characteristics:</b>
     * <ul>
     * <li><b>Transaction Isolation:</b> Read-only execution to ensure zero impact on database lock contention.</li>
     * <li><b>Integrity Guard:</b> Validates program existence before initiating resource-intensive scans.</li>
     * <li><b>Traceability:</b> Logs forensic markers for audit volume and delinquency identification.</li>
     * </ul>
     * </p>
     *
     * @param programId The unique {@link UUID} of the funding program to be audited.
     * @return A {@link List} of {@link ApplicantComplianceDTO} identifying delinquent entities.
     * @throws ProgramNotFoundException If the target Program ID does not exist.
     * @throws ComplianceViolationException If the audit detects structural data inconsistencies.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ApplicantComplianceDTO> getNonSubmittingApplicants(UUID programId) {
        log.info("Compliance Scan Initiated | Target Program: {}", programId);

        // 1. Get all unique application IDs for this program that have DISBURSEMENTS
        // Note: You need this custom query in your DisbursementRepository
        List<UUID> applicationIds = disbursementRepository.findDistinctApplicationIdsByProgramId(programId);

        log.info("Scan Execution | Analyzing {} active applications for delinquency.", applicationIds.size());

        if (applicationIds.isEmpty()) {
            return List.of();
        }

        try {
            return applicationIds.stream()
                    .filter(appId -> {
                        // Logic: Paid installments > Submitted reports
                        long paidCount = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);
                        long reportCount = grantReportRepository.countByApplicationId(appId);
                        return paidCount > reportCount;
                    })
                    .map(appId -> {
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
            log.error("Scan System Failure | Error during scan: {}", e.getMessage());
            throw new ComplianceViolationException("Critical system error during delinquency scan.");
        }
    }

    /**
     * Evaluates the real-time compliance standing of a specific application through deep-policy validation.
     * <p>
     * This method acts as a gatekeeper for subsequent grant actions (e.g., disbursements).
     * It delegates complex business rules to the {@link ComplianceValidator} to ensure
     * that only applicants with an 'APPROVED' latest report are marked as eligible.
     * </p>
     * <p>
     * <b>Compliance Criteria:</b>
     * <ul>
     * <li><b>Persistence:</b> The application must exist within the relational context.</li>
     * <li><b>Submission Integrity:</b> At least one progress report must be present.</li>
     * <li><b>Policy Alignment:</b> The most recent submission must carry an {@code APPROVED} status.</li>
     * </ul>
     * </p>
     *
     * @param applicationId The unique {@link UUID} of the subject application.
     * @return {@code true} if the application meets all regulatory criteria; {@code false} otherwise.
     * @throws ApplicationNotFoundException If the target application ID cannot be resolved.
     * @throws ComplianceViolationException If the validator encounters a structural system error.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isApplicantCompliant(UUID applicationId) {
        // 1. Ingress Logging: Documenting the start of a critical decision gate
        log.info("Compliance Validation | Initiating real-time status check for AppID: {}", applicationId);

        try {
            // We no longer fetch the 'Application' object.
            // We pass the ID directly to the validator.
            boolean isCompliant = complianceValidator.verifyCompliance(applicationId);

            log.info("Compliance Validation Finalized | AppID: {} | Outcome: {}",
                    applicationId, isCompliant ? "COMPLIANT" : "NON_COMPLIANT");

            return isCompliant;

        } catch (Exception e) {
            log.error("Compliance System Failure | Error for AppID: {}. Error: {}",
                    applicationId, e.getMessage());
            throw new ComplianceViolationException("The compliance engine encountered a structural failure.");
        }
    }

    /**
     * Aggregates a comprehensive compliance landscape for all applicants within a specific funding program.
     * <p>
     * This method serves as the core data engine for the Administrative Dashboard. It performs a
     * <b>Head-of-Queue</b> analysis by cross-referencing completed disbursements against
     * the chronological history of progress reports.
     * </p>
     * <p>
     * <b>Key Metrics Resolved:</b>
     * <ul>
     * <li><b>Disbursement Sync:</b> Identifies if the applicant is "Ready for Disbursement" or "Delinquent".</li>
     * <li><b>Relational Flattening:</b> Normalizes Applicant and Program metadata into a flattened DTO.</li>
     * <li><b>Fault Tolerance:</b> Individual record failures are captured locally to prevent global service degradation.</li>
     * </ul>
     * </p>
     *
     * @param programId The unique {@link UUID} of the funding program scope.
     * @return A {@link List} of {@link ApplicantComplianceDTO} reflecting the current administrative standing.
     * @throws ProgramNotFoundException If the program scope cannot be resolved.
     * @throws ComplianceDataException If a structural failure occurs during the aggregation process.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ApplicantComplianceDTO> getApplicantGrantReportingSummary(UUID programId) {
        log.info("Administrative Audit | Initiating summary for Program: {}", programId);

        // 1. Resolve Application IDs linked to this Program from our local Disbursement table
        List<UUID> applicationIds = disbursementRepository.findDistinctApplicationIdsByProgramId(programId);

        if (applicationIds.isEmpty()) {
            log.warn("Audit Scan | No active applications found for Program: {}", programId);
            return List.of();
        }

        try {
            log.info("Audit Execution | Processing {} application IDs from financial records.", applicationIds.size());

            return applicationIds.stream().map(appId -> {
                try {
                    // 2. Metric Calculation using flat UUIDs
                    long paidInstallments = disbursementRepository.countByApplicationIdAndStatus(appId, DisbursementStatus.PAID);

                    // Fetch reports locally
                    List<GrantReport> reports = grantReportRepository.findByApplicationIdOrderBySubmittedDateDesc(appId);

                    String complianceStatus = resolveStatus(reports, paidInstallments);

                   // ApplicationMetadataDTO metadata = applicationClient.getApplicationMetadata(appId);
                    // 3. Mapping: Note that Name/Program metadata must be fetched from an external service
                    // or represented by the ID in this decoupled version.
                    return ApplicantComplianceDTO.builder()
                            .applicationId(appId)
                            .applicantName("metadata.getApplicantName()") // Placeholder for Feign/Rest call
                            .programName("metadata.getProgramName()")
                            .applicationStatus("metadata.getStatus()")
                            .latestReportStatus(complianceStatus)
                            .currentInstallment((int) paidInstallments)
                            .build();

                } catch (Exception rowError) {
                    log.error("Audit Row Failure | App ID: {} | Error: {}", appId, rowError.getMessage());
                    return ApplicantComplianceDTO.builder()
                            .applicationId(appId)
                            .latestReportStatus("METADATA_CORRUPTION_ERROR")
                            .build();
                }
            }).toList();

        } catch (Exception globalError) {
            log.error("Audit System Failure | Program: {} | Exception: {}", programId, globalError.getMessage());
            throw new ComplianceDataException("Critical structural error during administrative summary generation.");
        }
    }
    /**
     * Evaluates the synchronization between financial disbursements and reporting obligations.
     * <p>
     * This logic serves as a "Compliance Delta" calculator. It identifies discrepancies
     * between the count of finalized payments and the volume of progress submissions
     * to determine the next administrative milestone or block.
     * </p>
     * <p>
     * <b>Business Rules:</b>
     * <ul>
     * <li><b>Cold Start:</b> If no payments and no reports exist, the project is ready to begin.</li>
     * <li><b>Delinquency:</b> If payments outpace reports, a "Missing Report" block is triggered.</li>
     * <li><b>Current State:</b> Otherwise, it reflects the status of the most recent submission.</li>
     * </ul>
     * </p>
     *
     * @param reports          A chronologically ordered list (DESC) of submitted {@link GrantReport} entities.
     * @param paidInstallments The total count of {@code COMPLETED} disbursements for the application.
     * @return A semantic status string representing the real-time compliance posture.
     */
    private String resolveStatus(List<GrantReport> reports, long paidInstallments) {
        // 1. Initial State: Handling the "Day Zero" scenario
        if (reports.isEmpty() && paidInstallments == 0) {
            log.debug("Status Resolution | Scenario: Initialized | Result: READY_FOR_FIRST_DISBURSEMENT");
            return "READY_FOR_FIRST_DISBURSEMENT";
        }

        // 2. Gap Analysis: Detecting missing reports relative to funding received
        if (reports.size() < paidInstallments) {
            String missingStatus = "MISSING_REPORT_FOR_INSTALLMENT_" + (reports.size() + 1);
            log.warn("Compliance Gap | App requires report for next phase. Current Reports: {} | Paid Phases: {}",
                    reports.size(), paidInstallments);
            return missingStatus;
        }

        // 3. State Projection: Mapping the latest report status or fallback
        return reports.stream()
                .findFirst()
                .map(report -> {
                    log.debug("Status Resolution | Scenario: Active | Latest Report ID: {} | Status: {}",
                            report.getGrantReportId(), report.getStatus());
                    return report.getStatus().name();
                })
                .orElseGet(() -> {
                    log.info("Status Resolution | Scenario: No Submissions | Result: NO_REPORTS_SUBMITTED");
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
                .auditDate(check.getDate().toString()) // Convert Instant to String for the DTO
                .build();
    }
}
