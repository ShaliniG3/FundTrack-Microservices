package com.cts.fundtrack.disbursement.repository;


import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.common.models.enums.GrantReportStatus;
import com.cts.fundtrack.disbursement.models.GrantReport;

/**
 * Spring Data JPA repository for {@link GrantReport} entities.
 * <p>
 * Provides data access operations for grant progress report records. Reports are
 * submitted by applicants to document their utilisation of disbursed funds and are
 * central to the compliance audit workflow. This repository is used by the report
 * submission service, compliance validator, and compliance dashboard aggregations.
 * </p>
 */
@Repository
public interface GrantReportRepository extends JpaRepository<GrantReport, UUID> {

    /**
     * Retrieves all grant reports linked to a specific application.
     * <p>
     * Refactored for the microservice architecture — no longer joins with the
     * Application table; uses a plain UUID reference instead.
     * </p>
     *
     * @param applicationId the UUID of the grant application
     * @return a list of all {@link GrantReport} entities for the application;
     *         an empty list if none have been submitted
     */
    List<GrantReport> findByApplicationId(UUID applicationId);

    /**
     * Counts all grant reports for a given application, regardless of status.
     * Used in reporting-window and compliance gap-analysis calculations.
     *
     * @param applicationId the UUID of the grant application
     * @return the total number of reports submitted for the application
     */
    long countByApplicationId(UUID applicationId);

    /**
     * Retrieves all grant reports for an application ordered by submission date
     * descending (most recent first). Used to determine the latest report status
     * during compliance checks.
     *
     * @param applicationId the UUID of the grant application
     * @return a list of {@link GrantReport} entities ordered newest-first
     */
    List<GrantReport> findByApplicationIdOrderBySubmittedDateDesc(UUID applicationId);

    /**
     * Counts grant reports for an application that are in a specific lifecycle status.
     * Used to filter for approved reports during compliance verification.
     *
     * @param applicationId the UUID of the grant application
     * @param status        the {@link GrantReportStatus} to filter by (e.g., {@code APPROVED})
     * @return the number of reports matching the given application and status
     */
    long countByApplicationIdAndStatus(UUID applicationId, GrantReportStatus status);

}