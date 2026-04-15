package com.cts.fundtrack.analytics.aspect;

import java.util.UUID;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.AuditClient;
import com.cts.fundtrack.common.dto.AuditRequestDTO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditClient auditClient;
    private final HttpServletRequest request;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditAction(Auditable auditable, Object result) {
        try {
            // 1. Extract User ID (DTO needs UUID)
            String xUserId = request.getHeader("X-User-Id");
            UUID userId = (xUserId != null) ? UUID.fromString(xUserId) : null;

            // 2. Extract Entity ID (Logic depends on what your Analytics methods return)
            // If you return a DTO with an 'id', we try to grab it here
            UUID entityId = null;
            
            // This is where you'd add logic for Analytics-specific return types
            // For example, if you're auditing a Program report:
            /* if (result instanceof SomeAnalyticsDTO dto) {
                   entityId = dto.getProgramId();
               } */

            // 3. Build the Audit DTO using your common AuditRequestDTO
            AuditRequestDTO auditLog = AuditRequestDTO.builder()
                    .userId(userId)
                    .action(auditable.action())
                    .entityName(auditable.entityName())
                    .entityId(entityId) 
                    .build();

            // 4. Fire and Forget to Identity Service
            auditClient.sendAuditLog(auditLog);
            
            log.info("Audit Log Dispatched | Action: {} | User: {}", 
                     auditable.action(), userId);

        } catch (Exception e) {
            // Protect the main transaction!
            log.error("AUDIT ERROR: Propagation failed. Reason: {}", e.getMessage());
        }
    }
}