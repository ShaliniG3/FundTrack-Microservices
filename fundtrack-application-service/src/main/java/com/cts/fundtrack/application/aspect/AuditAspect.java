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

/**
 * AOP aspect that intercepts service methods annotated with {@link Auditable} and
 * publishes a structured audit record to the centralised Audit Service.
 *
 * <p>After every successful method invocation the aspect extracts the acting user's
 * ID from the {@code X-User-Id} request header, determines the affected entity ID
 * from the return value, and forwards an {@link AuditRequestDTO} to the
 * {@link AuditClient}. Any failure inside the audit pipeline is swallowed so that
 * core business logic is never disrupted by audit-logging errors.</p>
 *
 * <p>If the {@code X-User-Id} header is absent (e.g., system-initiated actions),
 * the well-known nil UUID {@code 00000000-0000-0000-0000-000000000000} is used as
 * the actor identifier.</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditClient auditClient;
    private final HttpServletRequest request;

    // Standard industry way to represent a non-user action if header is missing
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * Intercepts the return of any method annotated with {@link Auditable} and
     * sends a corresponding audit log entry to the Audit Service.
     *
     * <p>The method resolves the acting user from the {@code X-User-Id} HTTP
     * header and the affected entity ID by inspecting the runtime type of
     * {@code result}. Supported return types are {@link Application},
     * {@link Decision}, {@link Review}, and {@link Recommendation}.</p>
     *
     * @param joinPoint  the join point that exposes reflective information about
     *                   the intercepted method call
     * @param auditable  the {@link Auditable} annotation instance carrying the
     *                   {@code action} and {@code entityName} metadata
     * @param result     the value returned by the intercepted method; used to
     *                   derive the entity ID for the audit record
     */
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