package com.cts.fundtrack.analytics.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.DailyAnalysisDTO;
import com.cts.fundtrack.common.dto.FinanceSummaryDTO;
import com.cts.fundtrack.common.dto.StatusDistributionDTO;

/**
 * Service interface for program-level analytics and financial reporting.
 * <p>
 * This service acts as an aggregator, collecting data from various microservices
 * (Grants, Finance) to provide high-level insights into program performance,
 * application processing trends, and budget utilization.
 * </p>
 */
public interface AnalyticsService {

    /**
     * Retrieves the distribution of application statuses for a specific program.
     * <p>
     * Aggregates counts for statuses such as SUBMITTED, APPROVED, and REJECTED
     * to provide a snapshot of the current application pipeline.
     * </p>
     *
     * @param programId the unique identifier of the program to analyze
     * @return a {@link StatusDistributionDTO} containing status categories and their counts
     */
    StatusDistributionDTO getStatusDistribution(UUID programId);

    /**
     * Retrieves a time-series analysis of application activity on a daily basis.
     * <p>
     * Provides counts of applications in various stages grouped by their submission date,
     * allowing for trend analysis over time.
     * </p>
     *
     * @param programId the unique identifier of the program to analyze
     * @return a list of {@link DailyAnalysisDTO} objects ordered by date
     */
    List<DailyAnalysisDTO> getDailyAnalysis(UUID programId);

    /**
     * Provides a comprehensive financial overview and budget utilization for a program.
     * <p>
     * Calculates total budget, funds disbursed (paid), funds committed (scheduled),
     * and the overall percentage of budget utilization.
     * </p>
     *
     * @param programId the unique identifier of the program to analyze
     * @return a {@link FinanceSummaryDTO} containing financial metrics
     */
    FinanceSummaryDTO getFinanceSummary(UUID programId);
}