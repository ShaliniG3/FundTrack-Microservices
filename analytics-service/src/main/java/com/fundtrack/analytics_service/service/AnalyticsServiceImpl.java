package com.fundtrack.analytics_service.service;

import com.fundtrack.analytics_service.client.ApplicationClient;
import com.fundtrack.analytics_service.client.FinanceClient;
import com.fundtrack.analytics_service.dto.*;
import com.fundtrack.analytics_service.dto.applicationdto.ApplicationResponseDTO;
import com.fundtrack.analytics_service.dto.programdto.ProgramResponseDTO;
import com.fundtrack.analytics_service.dto.disbursmentdto.DisbursementResponseDTO;
import com.fundtrack.analytics_service.model.external.GrantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for aggregating and processing program analytics.
 * <p>
 * This service communicates with the Grant and Finance microservices to fetch raw
 * data and performs in-memory aggregation to provide status distributions,
 * time-series analysis, and financial summaries.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ApplicationClient applicationClient;
    private final FinanceClient financeClient;

    /**
     * Calculates the distribution of application statuses for a specific program.
     * <p>
     * Fetches all applications for the program and groups them by their current
     * {@link GrantStatus}.
     * </p>
     *
     * @param programId Unique identifier of the program.
     * @return {@link StatusDistributionDTO} containing a list of status counts.
     */
    @Override
    public StatusDistributionDTO getStatusDistribution(UUID programId) {
        log.info("Fetching status distribution for programId: {}", programId);
        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);

        // Map the status during grouping to ensure types match
        Map<GrantStatus, Long> counts = apps.stream()
                .collect(Collectors.groupingBy(
                        app -> GrantStatus.valueOf(app.getStatus().name()), // 👈 Convert Enum type here
                        Collectors.counting()
                ));

        List<StatusCountDTO> statusCounts = counts.entrySet().stream()
                .map(e -> new StatusCountDTO(e.getKey(), e.getValue()))
                .toList();

        return new StatusDistributionDTO(programId, statusCounts);
    }
    /**
     * Performs a daily time-series analysis of application submissions.
     * <p>
     * Groups applications by their submission date and calculates counts for
     * key lifecycle statuses (Submitted, Under Review, Approved, Rejected).
     * </p>
     *
     * @param programId Unique identifier of the program.
     * @return A sorted list of {@link DailyAnalysisDTO} objects representing daily activity.
     */
    @Override
    public List<DailyAnalysisDTO> getDailyAnalysis(UUID programId) {
        log.info("Fetching daily analysis for programId: {}", programId);
        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);

        // Group applications by Date (LocalDate part of LocalDateTime)
        Map<LocalDate, List<ApplicationResponseDTO>> groupedByDate = apps.stream()
                .collect(Collectors.groupingBy(app ->
                        app.getSubmittedDate()
                                .atZone(ZoneId.systemDefault()) // 1. Apply a time zone
                                .toLocalDate()                  // 2. Extract the date
                ));

        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<ApplicationResponseDTO> dailyApps = entry.getValue();

                    return new DailyAnalysisDTO(
                            date.toString(),
                            countByStatus(dailyApps, "SUBMITTED"),
                            countByStatus(dailyApps, "UNDER_REVIEW"),
                            countByStatus(dailyApps, "APPROVED"),
                            countByStatus(dailyApps, "REJECTED")
                    );
                })
                .sorted(Comparator.comparing(DailyAnalysisDTO::getDate))
                .toList();
    }

    /**
     * Generates a financial summary.
     * Note: This version uses the 'byApplication' approach if 'byProgram' is unavailable.
     */
    @Override
    public FinanceSummaryDTO getFinanceSummary(UUID programId) {
        log.info("Generating finance summary for programId: {}", programId);

        // 1. Fetch Program and Applications
        ProgramResponseDTO program = applicationClient.getProgramDetails(programId);
        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);

        // 2. Fetch all disbursements for all applications in this program
        // This aggregates all disbursements into one list
        List<DisbursementResponseDTO> allDisbursements = apps.stream()
                .map(app -> financeClient.getDisbursementsByApplication(app.getApplicationId()))
                .flatMap(List::stream)
                .toList();

        // 3. Calculate total disbursed (PAID)
        double totalPaid = allDisbursements.stream()
                .filter(d -> "PAID".equalsIgnoreCase(String.valueOf(d.getStatus())))
                .mapToDouble(DisbursementResponseDTO::getAmount).sum();

        // 4. Calculate total committed (SCHEDULED)
        double totalCommitted = allDisbursements.stream()
                .filter(d -> "SCHEDULED".equalsIgnoreCase(String.valueOf(d.getStatus())))
                .mapToDouble(DisbursementResponseDTO::getAmount).sum();

        // 5. Count approved applications
        long approvedCount = apps.stream()
                .filter(a -> GrantStatus.APPROVED.equals(a.getStatus()))
                .count();

        double budget = (program.getBudget() != null) ? program.getBudget() : 0.0;

        return FinanceSummaryDTO.builder()
                .programId(programId)
                .totalBudget(budget)
                .totalFundsDisbursed(totalPaid)
                .fundsRemaining(budget - totalPaid)
                .fundsCommitted(totalCommitted)
                .approvedGrants(approvedCount)
                .utilisationPercentage(budget > 0 ? (totalPaid / budget) * 100 : 0)
                .build();
    }

    /**
     * Helper method to count applications within a list that match a specific status string.
     *
     * @param apps List of {@link ApplicationResponseDTO} to filter.
     * @param status The string name of the status to count.
     * @return The count of matching applications.
     */
    private Long countByStatus(List<ApplicationResponseDTO> apps, String status) {
        return apps.stream()
                .filter(a -> status.equals(a.getStatus().name()))
                .count();
    }
}