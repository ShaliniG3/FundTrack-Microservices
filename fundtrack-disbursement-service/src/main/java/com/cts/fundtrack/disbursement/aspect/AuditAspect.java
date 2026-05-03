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

/**
 * Spring AOP aspect that intercepts service methods annotated with
 * {@link com.cts.fundtrack.common.aspect.Auditable} and dispatches structured
 * audit log entries to the centralised FundTrack Audit Service.
 * <p>
 * This aspect is responsible for the disbursement service's contribution to the
 * platform-wide immutable audit trail. After any annotated method returns successfully,
 * it extracts the authenticated user's identity from the gateway-injected
 * {@code X-User-Id} request header, resolves the affected entity's UUID from the
 * method's return value, and sends an {@link com.cts.fundtrack.common.dto.AuditRequestDTO}
 * to the Audit Service via {@link com.cts.fundtrack.common.client.AuditClient}.
 * </p>
 * <p>
 * Audit failures are caught and logged as non-critical errors so that they never
 * propagate back to disrupt the primary business transaction.
 * </p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditClient auditClient;
    private final HttpServletRequest request;

    /**
     * After-returning advice that fires on any method annotated with
     * {@link com.cts.fundtrack.common.aspect.Auditable}.
     * <p>
     * Extracts the authenticated user ID from the {@code X-User-Id} request header,
     * resolves the affected entity's UUID from the method return value (or fall back
     * to the first method argument if the result is void), and dispatches a structured
     * audit log entry to the Audit Service.
     * </p>
     *
     * @param joinPoint  the AOP join point providing access to method arguments
     * @param auditable  the {@link com.cts.fundtrack.common.aspect.Auditable} annotation
     *                   instance, containing the action type and entity name
     * @param result     the value returned by the intercepted method; used to resolve
     *                   the affected entity's UUID
     */
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

    /**
     * Resolves the UUID of the primary entity affected by the audited operation.
     * <p>
     * Inspects the method's return value using pattern matching to extract the
     * relevant entity identifier from known disbursement domain types. For void
     * operations (e.g., deletes or status updates) where the result is {@code null},
     * it falls back to the first method argument, which is expected to be the target
     * entity's UUID.
     * </p>
     *
     * @param result    the value returned by the audited method; may be {@code null}
     *                  for void operations
     * @param joinPoint the AOP join point providing access to the method's arguments
     * @return the resolved entity {@link UUID}, or {@code null} if it cannot be
     *         determined from the result or arguments
     */
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