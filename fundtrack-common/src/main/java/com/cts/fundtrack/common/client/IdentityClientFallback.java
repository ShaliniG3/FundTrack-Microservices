package com.cts.fundtrack.common.client;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Circuit-breaker fallback for {@link IdentityClient}.
 *
 * <p>Returns an empty list when the Identity Service is unreachable so that
 * role-based notification attempts degrade gracefully without failing the
 * calling business operation.</p>
 */
@Component
@Slf4j
public class IdentityClientFallback implements IdentityClient {

    @Override
    public List<UUID> getUserIdsByRole(String role) {
        log.warn("IdentityClient fallback: could not fetch user IDs for role '{}'. Notification skipped.", role);
        return List.of();
    }
}
