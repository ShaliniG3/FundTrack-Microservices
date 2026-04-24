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
import com.cts.fundtrack.analytics.client.ProgramClient;
import com.cts.fundtrack.common.aspect.Auditable; // 👈 IMPORT YOUR COMMON ANNOTATION
import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.dto.DailyAnalysisDTO;
import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.dto.FinanceSummaryDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;
import com.cts.fundtrack.common.dto.StatusCountDTO;
import com.cts.fundtrack.common.dto.StatusDistributionDTO;
import com.cts.fundtrack.common.models.enums.ActionType; // 👈 IMPORT ENUMS
import com.cts.fundtrack.common.models.enums.ApplicationStatus;
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
    private final ProgramClient programClient;

    @Override
    @Auditable(action = ActionType.READ, entityName = EntityType.PROGRAM) // 👈 AUDIT ENABLED
    public StatusDistributionDTO getStatusDistribution(UUID programId) {
        log.info("Fetching status distribution for programId: {}", programId);
        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);

        // DRAFT has no analytics meaning; ACCEPTED is treated as APPROVED (offer confirmed)
        Map<GrantStatus, Long> counts = apps.stream()
                .filter(app -> app.getStatus() != ApplicationStatus.DRAFT)
                .collect(Collectors.groupingBy(
                        app -> app.getStatus() == ApplicationStatus.ACCEPTED
                                ? GrantStatus.APPROVED
                                : GrantStatus.valueOf(app.getStatus().name()),
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
                .filter(app -> app.getSubmittedDate() != null)
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

        ProgramResponseDTO program = programClient.getProgramDetails(programId);

        if (program == null) {
            log.warn("Program details unavailable for programId: {} — returning empty summary", programId);
            return FinanceSummaryDTO.builder()
                    .programId(programId)
                    .totalBudget(0.0)
                    .totalFundsDisbursed(0.0)
                    .fundsRemaining(0.0)
                    .fundsCommitted(0.0)
                    .approvedGrants(0L)
                    .utilisationPercentage(0.0)
                    .build();
        }

        List<ApplicationResponseDTO> apps = applicationClient.getApplicationsByProgram(programId);
        log.info("Apps fetched: count={}", apps != null ? apps.size() : "null");

        List<DisbursementResponseDTO> allDisbursements = apps.stream()
                .map(app -> financeClient.getDisbursementsByApplication(app.getApplicationId()))
                .flatMap(List::stream)
                .toList();
        log.info("Total disbursements fetched: {}", allDisbursements.size());

        double totalPaid = allDisbursements.stream()
                .filter(d -> "PAID".equalsIgnoreCase(String.valueOf(d.getStatus())))
                .mapToDouble(DisbursementResponseDTO::getAmount).sum();

        double totalCommitted = allDisbursements.stream()
                .filter(d -> "SCHEDULED".equalsIgnoreCase(String.valueOf(d.getStatus())))
                .mapToDouble(DisbursementResponseDTO::getAmount).sum();

        long approvedCount = apps.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.APPROVED
                        || a.getStatus() == ApplicationStatus.ACCEPTED)
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
                .filter(a -> {
                    String name = a.getStatus().name();
                    // ACCEPTED is counted alongside APPROVED in analytics charts
                    if ("APPROVED".equals(status)) {
                        return "APPROVED".equals(name) || "ACCEPTED".equals(name);
                    }
                    return status.equals(name);
                })
                .count();
    }
}