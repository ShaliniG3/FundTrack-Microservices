package com.cts.fundtrack.identity.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.UserMetadataDTO;
import com.cts.fundtrack.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal REST controller exposing lightweight user identity endpoints for
 * service-to-service communication within the FundTrack platform.
 *
 * <p>These endpoints are NOT intended for external/public consumption.
 * They are called exclusively by the Application Service via Feign client
 * to resolve applicant names from UUIDs when building
 * {@link com.cts.fundtrack.common.dto.ApplicationMetadataDTO} responses.</p>
 *
 * <p>Base path: {@code /api/internal/users}</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserRepository userRepository;

    /**
     * Resolves a user's display name and email from their UUID.
     *
     * <p>Called by the Application Service during {@code getApplicationMetadata()}
     * to populate the {@code applicantName} field. Returns 404 if the user
     * does not exist, triggering the {@link com.cts.fundtrack.application.client.IdentityServiceFallback}
     * in the Application Service which returns a safe stub name.</p>
     *
     * @param userId the UUID of the user to resolve
     * @return {@link UserMetadataDTO} with name and email, or 404 if not found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserMetadataDTO> getUserById(@PathVariable UUID userId) {
        log.debug("Internal user resolution request for ID: {}", userId);
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(
                        UserMetadataDTO.builder()
                                .userId(user.getUserId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .build()))
                .orElseGet(() -> {
                    log.warn("User not found for internal resolution: {}", userId);
                    return ResponseEntity.notFound().build();
                });
    }
}