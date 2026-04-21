package com.cts.fundtrack.identity.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.models.enums.Role;
import com.cts.fundtrack.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal REST controller that exposes user identity queries for
 * service-to-service calls within the FundTrack platform.
 *
 * <p>These endpoints are whitelisted in {@link com.cts.fundtrack.identity.config.SecurityConfig}
 * ({@code /api/v1/internal/**}) so they can be reached by other microservices
 * via Feign clients without a Bearer token.</p>
 *
 * <p>Base path: {@code /api/v1/internal/users}</p>
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Slf4j
public class UserInternalController {

    private final UserRepository userRepository;

    /**
     * Returns the UUIDs of all registered users that have the specified role.
     *
     * <p>Called by microservices (Application, Disbursement) when they need to
     * broadcast a notification to every user of a given role — e.g., alerting all
     * REVIEWERs that a new application has been submitted.</p>
     *
     * @param role the role name string (case-insensitive); must match a value in
     *             {@link Role}
     * @return list of user UUIDs with the given role; empty list if none exist
     *         or if the role name is not recognised
     */
    @GetMapping("/by-role/{role}")
    public List<UUID> getUserIdsByRole(@PathVariable String role) {
        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            List<UUID> ids = userRepository.findByRole(roleEnum)
                    .stream()
                    .map(u -> u.getUserId())
                    .toList();
            log.debug("Internal lookup: {} users found with role {}", ids.size(), role);
            return ids;
        } catch (IllegalArgumentException e) {
            log.warn("Internal lookup: unrecognised role '{}', returning empty list", role);
            return List.of();
        }
    }
}
