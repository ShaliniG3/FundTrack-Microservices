package com.cts.fundtrack.common.client;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.cts.fundtrack.common.config.FeignConfig;

/**
 * Feign client for internal communication with the {@code fundtrack-identity-service}.
 *
 * <p>Used by other microservices to query user identity data — specifically
 * to look up all user IDs that belong to a given role (e.g., REVIEWER, APPROVER,
 * FINANCE_OFFICER, COMPLIANCE_OFFICER) so that role-based broadcast notifications
 * can be dispatched without coupling business services to the user database.</p>
 *
 * <p>Falls back to {@link IdentityClientFallback} when the Identity Service is
 * unavailable, returning an empty list so the caller can continue gracefully.</p>
 */
@FeignClient(
    name = "fundtrack-identity-service",
    contextId = "identityClient",
    configuration = FeignConfig.class,
    fallback = IdentityClientFallback.class
)
public interface IdentityClient {

    /**
     * Returns the UUIDs of all users registered with the specified role.
     *
     * @param role the role name (e.g., {@code "REVIEWER"}, {@code "APPROVER"})
     * @return list of user UUIDs matching the role; empty if none or on error
     */
    @GetMapping("/api/v1/internal/users/by-role/{role}")
    List<UUID> getUserIdsByRole(@PathVariable("role") String role);
}
