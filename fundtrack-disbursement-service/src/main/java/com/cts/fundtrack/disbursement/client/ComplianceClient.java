package com.cts.fundtrack.disbursement.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig; // 1. IMPORT FROM COMMON

/**
 * Communicates with the Compliance Service to verify eligibility for payout.
 */
@FeignClient(
    name = "fundtrack-compliance-service", 
    configuration = FeignConfig.class, // 2. USE SHARED HARNESS
    fallback = ComplianceFallback.class
)
public interface ComplianceClient {

    @GetMapping("/api/v1/compliance/status/{applicationId}")
    boolean isApplicantCompliant(@PathVariable("applicationId") UUID applicationId);
}