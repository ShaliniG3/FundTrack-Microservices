package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig; // 1. IMPORT FROM COMMON

/**
 * Feign client for communicating with the FundTrack Compliance Service.
 * <p>
 * Provides a single internal endpoint for querying whether a grant applicant has
 * satisfied all reporting obligations required before the next disbursement can be
 * released. Uses the shared {@link FeignConfig} for authentication header propagation
 * and falls back to {@link ComplianceFallback} when the Compliance Service is unavailable.
 * </p>
 */
@FeignClient(
    name = "fundtrack-compliance-service",
    configuration = FeignConfig.class,
    fallback = ComplianceFallback.class
)
public interface ComplianceClient {

    /**
     * Checks whether the applicant associated with the given application has met all
     * compliance requirements (i.e., submitted and had approved the required grant reports).
     *
     * @param applicationId the UUID of the grant application to verify
     * @return {@code true} if the applicant is compliant and eligible for the next
     *         disbursement; {@code false} if they have outstanding reporting obligations
     *         or if the service is unreachable (fail-closed via {@link ComplianceFallback})
     */
    @GetMapping("/api/v1/compliance/status/{applicationId}")
    boolean isApplicantCompliant(@PathVariable("applicationId") UUID applicationId);
}