package com.cts.fundtrack.analytics.client;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.ApplicationResponseDTO;
import com.cts.fundtrack.common.models.enums.ApplicationStatus;

/**
 * Circuit breaker fallback for ApplicationClient.
 * Uses exact field names from ApplicationResponseDTO: userName and ApplicationStatus enum.
 */
@Component
public class ApplicationClientFallback implements ApplicationClient {

    private static final Logger log = LoggerFactory.getLogger(ApplicationClientFallback.class);

    @Override
    public List<ApplicationResponseDTO> getApplicationsByProgram(UUID programId) {
        log.warn("[CircuitBreaker] Application Service DOWN for programId={}. Providing mock data.", programId);

        List<ApplicationResponseDTO> mockApps = new ArrayList<>();

        // Using the exact fields: userName and ApplicationStatus enum
        mockApps.add(createMockApp(programId, ApplicationStatus.SUBMITTED, "System Mock User A"));
        mockApps.add(createMockApp(programId, ApplicationStatus.APPROVED, "System Mock User B"));
        mockApps.add(createMockApp(programId, ApplicationStatus.UNDER_REVIEW, "System Mock User C"));
        mockApps.add(createMockApp(programId, ApplicationStatus.SUBMITTED, "System Mock User D"));

        return mockApps;
    }

    /**
     * Helper to build mock applications.
     * Uses the Builder pattern since your DTO has @Builder.
     */
    private ApplicationResponseDTO createMockApp(UUID programId, ApplicationStatus status, String name) {
        return ApplicationResponseDTO.builder()
                .applicationId(UUID.randomUUID())
                .programId(programId)
                .userId(UUID.randomUUID())
                .userName(name) // Field name was corrected from applicantName to userName
                .status(status)   // Field type was corrected from String to ApplicationStatus
                .submittedDate(Instant.now())
                .build();
    }
}