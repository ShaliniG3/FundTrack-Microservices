package com.cts.fundtrack.disbursement.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import com.cts.fundtrack.common.config.FeignConfig; // 1. IMPORT FROM COMMON
import com.cts.fundtrack.common.dto.ApplicationMetadataDTO;

/**
 * Updated Client using the shared common FeignConfig.
 */
@FeignClient(
    name = "fundtrack-application-service", 
    configuration = FeignConfig.class, // 2. REFERENCE SHARED CONFIG
    fallback = ApplicationFallback.class
)
public interface ApplicationClient {

    @GetMapping("/api/internal/applications/{id}/metadata")
    ApplicationMetadataDTO getApplicationMetadata(@PathVariable("id") UUID id);

    @GetMapping("/api/internal/programs/{programId}/winners")
    List<UUID> getApprovedApplicationIds(@PathVariable("programId") UUID programId);

    @GetMapping("/api/internal/programs/{programId}/has-pending")
    Boolean hasPendingReviews(@PathVariable("programId") UUID programId);

    @PutMapping("/api/internal/applications/{id}/status/{newStatus}")
    void updateApplicationStatus(@PathVariable("id") UUID id, @PathVariable("newStatus") String newStatus);
}