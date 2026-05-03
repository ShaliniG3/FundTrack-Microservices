package com.cts.fundtrack.disbursement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.cts.fundtrack.common.dto.GrantReportRequestDTO;
import com.cts.fundtrack.common.dto.GrantReportResponseDTO;

/**
 * Service interface for managing the end-to-end lifecycle of grant utilization reporting.
 * <p>
 * Provides an abstraction for applicants to submit progress reports and evidence of fund usage.
 * It ensures reporting remains synchronized with the disbursement schedule and maintains
 * an immutable audit trail of all submitted proof.
 * </p>
 */
public interface GrantReportService {

    /**
     * Executes the submission of a new grant progress report.
     * <p>
     * This operation validates report data against the application's current lifecycle stage,
     * captures quantitative metrics, and securely persists binary proof (PDF) provided
     * by the applicant.
     * </p>
     *
     * @param dto   The {@link GrantReportRequestDTO} containing qualitative and quantitative progress data.
     * @param proof The {@link MultipartFile} representing the physical evidence (e.g., invoices, receipts).
     * @return A {@link GrantReportResponseDTO} containing the processed report details and unique identifier.
     */

    GrantReportResponseDTO submitGrantReport(GrantReportRequestDTO dto, MultipartFile proof);

    /**
     * Retrieves the complete reporting history for a specific grant application.
     * <p>
     * Serving both Applicants for progress tracking and Officers for compliance auditing,
     * this method provides a consolidated view of all historical submissions linked
     * to the specified application.
     * </p>
     *
     * @param applicationId The unique {@link UUID} of the grant application.
     * @return A list of {@link GrantReportResponseDTO} objects containing summarized report metadata.
     */

    List<GrantReportResponseDTO> getMyGrantReports(UUID applicationId);
}