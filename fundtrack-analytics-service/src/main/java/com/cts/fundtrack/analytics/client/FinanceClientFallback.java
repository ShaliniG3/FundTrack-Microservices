package com.cts.fundtrack.analytics.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.DisbursementResponseDTO;

/**
 * Circuit breaker fallback for {@link FinanceClient}.
 *
 * <p>Activated when the Finance/Disbursement Service is unreachable or returns
 * repeated errors. Returns an empty disbursement list so spend analytics can
 * degrade gracefully — showing zero disbursements rather than failing entirely.</p>
 */
@Component
public class FinanceClientFallback implements FinanceClient {

    private static final Logger log = LoggerFactory.getLogger(FinanceClientFallback.class);

    /**
     * Returns an empty list when the Finance Service is unavailable.
     * Analytics that compute total spend will report zero rather than throwing.
     *
     * @param applicationId the application whose disbursements were requested
     * @return an empty list
     */
    @Override
    public List<DisbursementResponseDTO> getDisbursementsByApplication(UUID applicationId) {
        log.warn("[CircuitBreaker] Finance Service unavailable — returning empty disbursements for applicationId={}", applicationId);
        return Collections.emptyList();
    }
}
