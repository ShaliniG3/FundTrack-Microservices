package com.cts.fundtrack.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.cts.fundtrack.common.config.FeignConfig; // 👈 Import your common config
import com.cts.fundtrack.common.dto.AuditRequestDTO;

/**
 * Feign Client for internal communication with the Identity Microservice.
 * name: must match 'spring.application.name' of the Identity Service in Eureka.
 * configuration: Ensures X-User headers are propagated automatically.
 */
@FeignClient(
    name = "fundtrack-identity-service", 
    configuration = FeignConfig.class // 👈 This is the "correct" link to your interceptor
)
public interface AuditClient {

    /**
     * Propagates an audit log request to the Identity Service's internal endpoint.
     * @param auditRequest The DTO containing action, entity info, and userId (UUID).
     */
    @PostMapping("/api/v1/internal/audit/logs")
    void sendAuditLog(@RequestBody AuditRequestDTO auditRequest);
}