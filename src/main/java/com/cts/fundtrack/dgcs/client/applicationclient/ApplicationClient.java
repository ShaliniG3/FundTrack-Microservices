package com.cts.fundtrack.dgcs.client.applicationclient;

import com.cts.fundtrack.dgcs.client.dto.ApplicationMetadataDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "application-service", fallback = ApplicationFallback.class)
public interface ApplicationClient {

    @GetMapping("/api/internal/applications/{id}/metadata")
    ApplicationMetadataDTO getApplicationMetadata(@PathVariable("id") UUID id);

    // NEW: Needed to find approved applicants for the budget split
    @GetMapping("/api/internal/programs/{programId}/winners")
    List<UUID> getApprovedApplicationIds(@PathVariable("programId") UUID programId);

    // NEW: Needed for the 'hasPendingReviews' check
    @GetMapping("/api/internal/programs/{programId}/has-pending")
    Boolean hasPendingReviews(@PathVariable("programId") UUID programId);

    // NEW: Needed to move the app from APPROVED to ACCEPTED after split
    @PutMapping("/api/internal/applications/{id}/status/{newStatus}")
    void updateApplicationStatus(@PathVariable("id") UUID id, @PathVariable("newStatus") String newStatus);
}