package com.cts.fundtrack.disbursement.aspect;

import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.AuditClient;
import com.cts.fundtrack.common.dto.AuditRequestDTO;
import com.cts.fundtrack.common.dto.DisbursementResponseDTO;
import com.cts.fundtrack.disbursement.models.Disbursement;
import com.cts.fundtrack.disbursement.models.Payment;

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
    public void logAuditAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            // 1. Extract User ID from Gateway Headers
            String xUserId = request.getHeader("X-User-Id");
            UUID userId = (xUserId != null) ? UUID.fromString(xUserId) : null;

            // 2. Resolve the ID of the financial record affected
            UUID entityId = resolveEntityId(result, joinPoint);

            // 3. Build and Dispatch to IAM
            AuditRequestDTO auditLog = AuditRequestDTO.builder()
                    .userId(userId)
                    .action(auditable.action())
                    .entityName(auditable.entityName())
                    .entityId(entityId)
                    .build();

            auditClient.sendAuditLog(auditLog);
            log.info("Financial Audit Dispatched | Action: {} | Entity: {} | ID: {}", 
                     auditable.action(), auditable.entityName(), entityId);

        } catch (Exception e) {
            log.error("Audit Logging Failed (Non-Critical): {}", e.getMessage());
        }
    }

    private UUID resolveEntityId(Object result, JoinPoint joinPoint) {
        // Handle Disbursement Entity or DTO
        if (result instanceof Disbursement d) return d.getDisbursementId();
        if (result instanceof DisbursementResponseDTO dto) return dto.getId();
        
        // Handle Payment Entity
        if (result instanceof Payment p) return p.getPaymentId();
        
        // Handle Delete/Void operations via method arguments
        if (result == null && joinPoint.getArgs().length > 0) {
            Object firstArg = joinPoint.getArgs()[0];
            if (firstArg instanceof UUID uuid) return uuid;
        }
        
        return null;
    }
}