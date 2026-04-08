package com.cts.fundtrack.dgcs.client.complianceclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "compliance-service", fallback = ComplianceFallback.class)
public interface ComplianceClient {
    @GetMapping("/api/internal/compliance/status/{applicationId}")
    boolean isApplicantCompliant(@PathVariable("applicationId") UUID applicationId);
}