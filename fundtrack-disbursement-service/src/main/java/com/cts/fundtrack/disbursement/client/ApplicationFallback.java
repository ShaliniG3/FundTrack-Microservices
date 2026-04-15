package com.cts.fundtrack.disbursement.client;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ApplicationMetadataDTO;

import lombok.extern.slf4j.Slf4j;



@Component
@Slf4j
    public class ApplicationFallback implements ApplicationClient {

        @Override
        public ApplicationMetadataDTO getApplicationMetadata(UUID id) {
            log.error("Fallback: Application Service unreachable for Metadata ID: {}", id);
            return ApplicationMetadataDTO.builder()
                    .applicantName("SYSTEM_TEMPORARILY_UNAVAILABLE").status("UNKNOWN").build();
        }

        @Override
        public List<UUID> getApprovedApplicationIds(UUID programId) {
            log.error("Fallback: Cannot fetch winners for Program: {}", programId);
            return Collections.emptyList(); // Return empty list so the loop doesn't crash
        }

        @Override
        public Boolean hasPendingReviews(UUID programId) {
            log.error("Fallback: Cannot verify pending reviews. Defaulting to TRUE (Safe mode)");
            return true; // Better to block a split if the service is down than to do it wrong
        }

        @Override
        public void updateApplicationStatus(UUID id, String newStatus) {
            log.error("Fallback: Failed to update status to {} for App: {}. Manual sync required.", newStatus, id);
        }
    }