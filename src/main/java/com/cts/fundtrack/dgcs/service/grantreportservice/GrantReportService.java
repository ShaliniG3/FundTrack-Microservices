package com.cts.fundtrack.dgcs.service.grantreportservice;

import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportRequestDTO;
import com.cts.fundtrack.dgcs.dto.grantreportdto.GrantReportResponseDTO;
import com.cts.fundtrack.dgcs.model.GrantReport;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing the end-to-end lifecycle of grant utilization reporting.
 * <p>
 * This service provides the necessary abstraction for applicants to submit progress reports
 * and evidence of the fund usage. It ensures that reporting remains synchronized with the
 * disbursement schedule and maintains an audit trail of all submitted proof.
 * </p>
 */
public interface GrantReportService {

    /**
     * Executes the submission of a new grant progress report.
     * <p>
     * This operation involves validating the report data against the application's
     * current lifecycle stage, capturing quantitative metrics, and securely
     * persisting binary proof (PDF) provided by the applicant.
     * </p>
     *
     * @param dto   The {@link GrantReportRequestDTO} containing qualitative and quantitative progress data.
     * @param proof The {@link MultipartFile} representing the physical evidence (e.g., invoices, PDFs).
     * @return The persisted {@link GrantReport} entity containing the generated report ID.
     */

    GrantReportResponseDTO submitGrantReport(GrantReportRequestDTO dto, MultipartFile proof);

    /**
     * Retrieves the complete reporting history for a specific grant application.
     * <p>
     * Used by both Applicants for tracking and Officers for auditing, this method
     * provides a consolidated view of all historical submissions linked to the
     * provided unique identifier.
     * </p>
     *
     * @param applicationId The raw {@link UUID} of the grant application.
     * @return A list of {@link GrantReportResponseDTO} objects containing summarized report metadata.
     */
    List<GrantReportResponseDTO> getMyGrantReports(UUID applicationId);

}