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

/**
 * AOP aspect that intercepts methods annotated with {@link Auditable} in the
 * Notification Service and forwards a corresponding audit log entry to the central
 * Identity Service via the shared {@link AuditClient} Feign client.
 *
 * <p>Because the Notification Service does not own a copy of the {@code User}
 * entity or an {@code audit_logs} table, audit records are delegated to the
 * Identity Service over HTTP rather than persisted locally.</p>
 *
 * <p>Actor resolution strategy:</p>
 * <ol>
 *   <li>The {@code X-User-Id} header injected by the API Gateway is parsed as a
 *       {@link UUID} and used as the actor identifier.</li>
 *   <li>If the header is absent, blank, or contains a malformed UUID, the well-known
 *       {@link #SYSTEM_USER_ID} sentinel ({@code 00000000-0000-0000-0000-000000000000})
 *       is used instead to represent system-automated notification events.</li>
 * </ol>
 *
 * <p>Audit failures are caught and logged as errors but never propagated to the
 * caller, ensuring that a downstream audit service outage does not prevent
 * notifications from being delivered.</p>
 *
 * @see Auditable
 * @see AuditClient
 * @see AuditRequestDTO
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditClient auditClient;
    private final HttpServletRequest request;

    /**
     * Well-known sentinel UUID used as the actor when no authenticated user
     * can be identified — typically for system-automated notification events.
     */
    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /**
     * Advice that fires after any method annotated with {@link Auditable} returns
     * successfully and forwards an audit log entry to the Identity Service.
     *
     * <p>Steps performed:</p>
     * <ol>
     *   <li>Extract the actor UUID from the {@code X-User-Id} gateway header;
     *       fall back to {@link #SYSTEM_USER_ID} on failure.</li>
     *   <li>Extract the {@link com.cts.fundtrack.notification.models.Notification}
     *       ID from the method return value if the result is a
     *       {@link com.cts.fundtrack.notification.models.Notification} instance.</li>
     *   <li>Build an {@link AuditRequestDTO} and dispatch it via
     *       {@link AuditClient#sendAuditLog}.</li>
     * </ol>
     *
     * <p>Any exception raised during audit processing is caught and logged;
     * it will never propagate to the caller.</p>
     *
     * @param joinPoint  the join point describing the intercepted method
     * @param auditable  the {@link Auditable} annotation instance containing
     *                   action and entity type metadata
     * @param result     the value returned by the intercepted method; used to
     *                   extract the notification entity UUID
     */
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