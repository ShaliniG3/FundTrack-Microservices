package com.cts.fundtrack.analytics.client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.common.models.enums.DisbursementStatus;

/**
 * Circuit breaker fallback for FinanceClient.
 * Provides mock disbursement data using the precise fields from DisbursementResponseDTO.
 */
@Component
public class FinanceClientFallback implements FinanceClient {

    private static final Logger log = LoggerFactory.getLogger(FinanceClientFallback.class);

    /**
     * Returns mock disbursements when the Finance Service is unavailable.
     * This ensures financial analytics calculations don't drop to zero.
     */
    @Override
    public List<DisbursementResponseDTO> getDisbursementsByApplication(UUID applicationId) {
        log.warn("[CircuitBreaker] Finance Service DOWN for applicationId={}. Providing mock disbursement data.", applicationId);

        List<DisbursementResponseDTO> mockDisbursements = new ArrayList<>();

        // Creating dummy installments to keep the charts populated
        mockDisbursements.add(createMockDisbursement(applicationId, 5000.00, DisbursementStatus.PAID, LocalDate.now().minusMonths(1)));
        mockDisbursements.add(createMockDisbursement(applicationId, 2500.00, DisbursementStatus.PENDING, LocalDate.now().plusDays(15)));

        return mockDisbursements;
    }

    /**
     * Helper to build mock disbursement records using the Builder pattern.
     */
    private DisbursementResponseDTO createMockDisbursement(UUID appId, Double amount, DisbursementStatus status, LocalDate date) {
        return DisbursementResponseDTO.builder()
                .id(UUID.randomUUID()) // Matches your 'id' field
                .applicationId(appId)
                .amount(amount)
                .status(status)
                .scheduledDate(date) // Matches your LocalDate field
                .build();
    }
}