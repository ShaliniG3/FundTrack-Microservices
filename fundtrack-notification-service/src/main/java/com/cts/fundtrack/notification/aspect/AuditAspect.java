package com.cts.fundtrack.notification.aspect;

import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.AuditClient;
import com.cts.fundtrack.common.dto.AuditRequestDTO;
import com.cts.fundtrack.notification.models.Notification;

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

    // Standard fallback for system-automated notifications
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logNotificationAudit(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            // 1. Extract User ID as UUID from the Gateway header
            String userIdStr = request.getHeader("X-User-Id");
            UUID userId;
            
            try {
                userId = (userIdStr != null && !userIdStr.isBlank()) 
                         ? UUID.fromString(userIdStr) 
                         : SYSTEM_USER_ID;
            } catch (IllegalArgumentException e) {
                log.warn("Mismatched UUID format in header: {}. Using SYSTEM_USER.", userIdStr);
                userId = SYSTEM_USER_ID;
            }

            // 2. Extract Notification ID from the result
            UUID entityId = null;
            if (result instanceof Notification notification) {
                // Assuming your field is notificationId or id
                entityId = notification.getNotificationId(); 
            }

            // 3. Construct the Audit DTO
            AuditRequestDTO auditLog = AuditRequestDTO.builder()
                    .userId(userId)
                    .action(auditable.action())
                    .entityId(entityId)
                    .entityName(auditable.entityName())
                    .build();

            // 4. Dispatch to Identity/Audit microservice
            auditClient.sendAuditLog(auditLog);
            
            log.info("Audit Success | Notification {} | Action: {}", entityId, auditable.action());

        } catch (Exception e) {
            // Fail-safe: Don't stop the notification from being sent if auditing fails
            log.error("Notification Audit failed: {}", e.getMessage());
        }
    }
}