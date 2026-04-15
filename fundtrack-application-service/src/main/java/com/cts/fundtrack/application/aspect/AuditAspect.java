package com.cts.fundtrack.application.aspect;

import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.application.model.Application;
import com.cts.fundtrack.application.model.Decision;
import com.cts.fundtrack.application.model.Recommendation;
import com.cts.fundtrack.application.model.Review;
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

    // Standard industry way to represent a non-user action if header is missing
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            // 1. Extract User ID from Gateway header and convert to UUID
            String userIdStr = request.getHeader("X-User-Id");
            UUID userId = (userIdStr != null) ? UUID.fromString(userIdStr) : SYSTEM_USER_ID;
            
            // 2. Extract Entity ID based on the returned object type
            UUID entityId = null;
            if (result instanceof Application app) {
                entityId = app.getApplicationId();
            } else if (result instanceof Decision decision) {
                entityId = decision.getDecisionId();
            } else if (result instanceof Review review) {
                entityId = review.getReviewId();
            } else if (result instanceof Recommendation rec) {
                entityId = rec.getRecommendationId();
            }

            // 3. Build the DTO matching your 4-field structure exactly
            AuditRequestDTO auditLog = AuditRequestDTO.builder()
                    .userId(userId)
                    .action(auditable.action())
                    .entityId(entityId)
                    .entityName(auditable.entityName())
                    .build();

            // 4. Send to Identity Service
            auditClient.sendAuditLog(auditLog);
            
            log.info("Successfully sent audit log for {} on {}", auditable.action(), auditable.entityName());

        } catch (Exception e) {
            log.error("Audit log propagation failed: {}", e.getMessage());
        }
    }
}