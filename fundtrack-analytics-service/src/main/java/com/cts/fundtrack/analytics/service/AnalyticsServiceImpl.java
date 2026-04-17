package com.cts.fundtrack.analytics.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cts.fundtrack.analytics.client.ApplicationClient;
import com.cts.fundtrack.analytics.client.FinanceClient;
import com.cts.fundtrack.common.aspect.Auditable; // 👈 IMPORT YOUR COMMON ANNOTATION
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.DailyAnalysisDTO;
import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.dto.FinanceSummaryDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.dto.StatusCountDTO;
import com.cts.fundtrack.common.dto.StatusDistributionDTO;
import com.cts.fundtrack.common.models.enums.ActionType; // 👈 IMPORT ENUMS
import com.cts.fundtrack.common.models.enums.EntityType;
import com.cts.fundtrack.common.models.enums.GrantStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link AnalyticsService}.
 *
 * <p>Aggregates data from two downstream Feign clients:</p>
 * <ul>
 *   <li>{@link ApplicationClient} — supplies application records and program metadata
 *       from the Application Service.</li>
 *   <li>{@link FinanceClient} — supplies disbursement records from the Finance Service.</li>
 * </ul>
 *
 * <p>All public methods are annotated with {@link com.cts.fundtrack.common.aspect.Auditable}
 * so that every analytics read operation is recorded in the central audit log via the
 * {@code AuditAspect}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ApplicationClient applicationClient;
    private final FinanceClient financeClient;

    @Override
    @Auditable(action = ActionType.READ, entityName = EntityType.PROGRAM) // 👈 AUDIT ENABLED
    public StatusDistributionDTO getStatusDistribution(UUID programId) {
        log.info("Fetching status distribution for programId: {}", programId);
        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);

        Map<GrantStatus, Long> counts = apps.stream()
                .collect(Collectors.groupingBy(
                        app -> GrantStatus.valueOf(app.getStatus().name()),
                        Collectors.counting()
                ));

        List<StatusCountDTO> statusCounts = counts.entrySet().stream()
                .map(e -> new StatusCountDTO(e.getKey(), e.getValue()))
                .toList();

        return new StatusDistributionDTO(programId, statusCounts);
    }

    @Override
    @Auditable(action = ActionType.READ, entityName = EntityType.PROGRAM) // 👈 AUDIT ENABLED
    public List<DailyAnalysisDTO> getDailyAnalysis(UUID programId) {
        log.info("Fetching daily analysis for programId: {}", programId);
        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);

        Map<LocalDate, List<ApplicationResponseDTO>> groupedByDate = apps.stream()
                .collect(Collectors.groupingBy(app ->
                        app.getSubmittedDate()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
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

    @Override
    @Auditable(action = ActionType.READ, entityName = EntityType.FINANCE) // 👈 AUDIT AS FINANCE ENTITY
    public FinanceSummaryDTO getFinanceSummary(UUID programId) {
        log.info("Generating finance summary for programId: {}", programId);

        ProgramResponseDTO program = applicationClient.getProgramDetails(programId);
        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);

        List<DisbursementResponseDTO> allDisbursements = apps.stream()
                .map(app -> financeClient.getDisbursementsByApplication(app.getApplicationId()))
                .flatMap(List::stream)
                .toList();

        double totalPaid = allDisbursements.stream()
                .filter(d -> "PAID".equalsIgnoreCase(String.valueOf(d.getStatus())))
                .mapToDouble(DisbursementResponseDTO::getAmount).sum();

        double totalCommitted = allDisbursements.stream()
                .filter(d -> "SCHEDULED".equalsIgnoreCase(String.valueOf(d.getStatus())))
                .mapToDouble(DisbursementResponseDTO::getAmount).sum();

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
     * Counts the number of applications whose status name matches the given string.
     *
     * @param apps   the list of applications to search through
     * @param status the exact status name to match (e.g., {@code "SUBMITTED"}, {@code "APPROVED"})
     * @return the count of applications with the specified status
     */
    private Long countByStatus(List<ApplicationResponseDTO> apps, String status) {
        return apps.stream()
                .filter(a -> status.equals(a.getStatus().name()))
                .count();
    }
}