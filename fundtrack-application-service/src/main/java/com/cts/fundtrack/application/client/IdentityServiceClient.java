package com.cts.fundtrack.application.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig;
import com.cts.fundtrack.common.dto.UserMetadataDTO;

/**
 * Feign client for communicating with the FundTrack Identity Service.
 *
 * <p>Provides internal service-to-service access to user identity data —
 * specifically applicant names and emails — required when building
 * {@link com.cts.fundtrack.common.dto.ApplicationMetadataDTO} responses
 * consumed by the Disbursement, Compliance, and Payment services.</p>
 *
 * <p>Uses the shared {@link FeignConfig} for authentication header propagation
 * and falls back to {@link IdentityServiceFallback} when the Identity Service
 * is unavailable, returning a safe stub so downstream services degrade
 * gracefully instead of throwing exceptions.</p>
 */
@FeignClient(
        name = "fundtrack-identity-service",
        contextId = "applicationIdentityClient",
        configuration = FeignConfig.class,
        fallback = IdentityServiceFallback.class
)
public interface IdentityServiceClient {

    /**
     * Resolves a user's display name and email from their UUID.
     *
     * <p>Called during {@code getApplicationMetadata()} to populate the
     * {@code applicantName} field in {@link com.cts.fundtrack.common.dto.ApplicationMetadataDTO}.
     * This ensures all downstream services (Disbursement, Compliance, Payment)
     * receive the correct applicant name without needing their own Identity
     * Service connection.</p>
     *
     * @param userId the UUID of the applicant to resolve
     * @return a {@link UserMetadataDTO} containing the user's name and email;
     *         returns a safe stub on fallback
     */
    @GetMapping("/api/internal/users/{userId}")
    UserMetadataDTO getUserById(@PathVariable("userId") UUID userId);
}