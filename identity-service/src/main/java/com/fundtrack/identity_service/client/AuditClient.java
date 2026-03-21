package com.fundtrack.identity_service.client;

import com.fundtrack.identity_service.dto.auditdto.AuditLogRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client for communicating with the Audit Microservice.
 * <p>
 * This client uses Spring Cloud OpenFeign to perform declarative REST calls
 * to the 'audit-service' registered in the Eureka Discovery Server.
 * It abstracts the complexity of Ribbon load balancing and HTTP execution.
 * </p>
 * * @author Gemini
 * @version 2026.1
 */
@FeignClient(name = "audit-service")
public interface AuditClient {

    /**
     * Dispatches a system event to the Audit Service for persistent logging.
     * <p>
     * This method sends a POST request containing the audit metadata (user, action,
     * entity, and timestamp details) to the remote audit endpoint.
     * </p>
     *
     * @param request the {@link AuditLogRequestDTO} containing the audit event details.
     * Must not be null and should comply with the Audit Service's
     * validation constraints.
     */
    @PostMapping("/api/v1/audit/log")
    void log(@RequestBody AuditLogRequestDTO request);
}