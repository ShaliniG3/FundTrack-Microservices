package com.cts.fundtrack.application.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.UserMetadataDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * Resilience4j fallback implementation for {@link IdentityServiceClient}.
 *
 * <p>Activated automatically when the FundTrack Identity Service is unreachable
 * or returns repeated errors. Returns a safe stub {@link UserMetadataDTO} so
 * that the Application Service can continue building
 * {@link com.cts.fundtrack.common.dto.ApplicationMetadataDTO} responses without
 * crashing — downstream services (Disbursement, Compliance, Payment) will see
 * a shortened UUID as the applicant name rather than an exception.</p>
 */
@Component
@Slf4j
public class IdentityServiceFallback implements IdentityServiceClient {

    /**
     * Returns a stub {@link UserMetadataDTO} indicating the Identity Service
     * is temporarily unavailable.
     *
     * <p>The fallback name is derived from the first 8 characters of the UUID
     * so it is still uniquely identifiable in the UI while clearly indicating
     * a resolution failure.</p>
     *
     * @param userId the UUID of the user that could not be resolved
     * @return a stub DTO with a shortened UUID as the name and {@code "N/A"} email
     */
    @Override
    public UserMetadataDTO getUserById(UUID userId) {
        log.error("Fallback: Identity Service unreachable — cannot resolve name for User ID: {}", userId);
        return UserMetadataDTO.builder()
                .userId(userId)
                .name("User-" + userId.toString().substring(0, 8).toUpperCase())
                .email("N/A")
                .build();
    }
}