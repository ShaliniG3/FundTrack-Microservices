package com.cts.fundtrack.dgcs.client.applicationclient;

import com.cts.fundtrack.dgcs.client.dto.ApplicationMetadataDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;



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