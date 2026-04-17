package com.cts.fundtrack.analytics.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig;
import com.cts.fundtrack.common.dto.DisbursementResponseDTO;

/**
 * Feign client for communicating with the <b>finance-service</b>.
 * <p>
 * This client provides access to financial data, specifically disbursements,
 * which are necessary for calculating funding analytics and spend tracking.
 */
@FeignClient(
    name = "finance-service",
    configuration = FeignConfig.class,
    fallback = FinanceClientFallback.class
)
public interface FinanceClient {

    /**
     * Retrieves a list of all disbursement records associated with a specific application.
     * <p>
     * Used by the analytics engine to aggregate total funds released versus
     * the allocated budget for a given application ID.
     *
     * @param applicationId The unique identifier (UUID) of the application.
     * @return A {@link List} of {@link DisbursementResponseDTO} containing transaction details.
     */
    @GetMapping("/api/v1/disbursements/application/{applicationId}")
    List<DisbursementResponseDTO> getDisbursementsByApplication(@PathVariable UUID applicationId);
}