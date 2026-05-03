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

/**
 * AOP aspect that intercepts service methods annotated with {@link Auditable}
 * and asynchronously dispatches audit log entries to the Identity Service.
 *
 * <p>This aspect runs {@code @AfterReturning} — meaning it only fires when the
 * intercepted method completes successfully. Exceptions in the audit path are
 * caught and logged locally so they never propagate to the business transaction.</p>
 *
 * <p>The user identity is extracted from the {@code X-User-Id} HTTP header injected
 * by the API Gateway. The entity ID is resolved from the method's return value when
 * applicable.</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditClient auditClient;
    private final HttpServletRequest request;

    /**
     * Advice that fires after a method annotated with {@link Auditable} returns normally.
     *
     * <p>Extracts the authenticated user's ID from the {@code X-User-Id} request header,
     * builds an {@link AuditRequestDTO}, and forwards it to the Identity Service via
     * {@link AuditClient}. All exceptions are swallowed to protect the primary transaction.</p>
     *
     * @param auditable the {@link Auditable} annotation instance carrying action/entity metadata
     * @param result    the value returned by the intercepted method (may be {@code null})
     */
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