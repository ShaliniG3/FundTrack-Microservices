package com.cts.fundtrack.identity.aspect;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.repository.AuditRepository;
import com.cts.fundtrack.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

/**
 * Aspect that intercepts methods annotated with {@link Auditable} and persists
 * a corresponding {@link AuditLog} entry in the identity service's own database.
 *
 * <p>This aspect operates <em>after</em> a method returns successfully
 * ({@code @AfterReturning}), ensuring that only successful operations are
 * recorded. Failures are swallowed silently so that audit logging never
 * disrupts the primary business flow.</p>
 *
 * <p>The actor (the user responsible for the action) is resolved in priority order:</p>
 * <ol>
 *   <li>The authenticated principal held in the current {@link org.springframework.security.core.context.SecurityContext}.</li>
 *   <li>A user whose ID can be extracted from the method's return value.</li>
 * </ol>
 *
 * <p>Audit entries capture the action type, entity type, entity UUID, the acting
 * user, and an ISO-8601 timestamp.</p>
 *
 * @see Auditable
 * @see AuditLog
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    /**
     * Advice that fires after any method annotated with {@link Auditable} returns
     * without throwing an exception.
     *
     * <p>It attempts to:</p>
     * <ol>
     *   <li>Extract a {@link UUID} representing the affected entity from {@code result}.</li>
     *   <li>Resolve the acting {@link User} from the security context or the entity UUID.</li>
     *   <li>Build and persist an {@link AuditLog} record.</li>
     * </ol>
     *
     * <p>Any exception raised during audit processing is caught and logged as a
     * warning — it will never propagate to the caller.</p>
     *
     * @param joinPoint  the join point describing the intercepted method
     * @param auditable  the {@link Auditable} annotation instance containing action metadata
     * @param result     the value returned by the intercepted method; used to extract the entity UUID
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            UUID entityId = extractUuidFromResult(result);
            if (entityId == null) return;

            User actor = resolveActor(entityId);
            if (actor == null) return;

            AuditLog logEntry = AuditLog.builder()
                    .user(actor)
                    .action(auditable.action())
                    .entityId(entityId)
                    .entityName(auditable.entityName())
                    .timestamp(Instant.now())
                    .build();

            auditRepository.save(logEntry);

        } catch (Exception e) {
            log.warn("Audit logging failed silently: {}", e.getMessage());
        }
    }

    /**
     * Resolves the {@link User} responsible for the audited action.
     *
     * <p>Resolution strategy:</p>
     * <ol>
     *   <li>If the security context contains an authenticated, non-anonymous principal,
     *       the user is looked up by email.</li>
     *   <li>If no authenticated principal is present (e.g., during a self-registration
     *       flow), the user is looked up by the provided entity UUID.</li>
     * </ol>
     *
     * @param entityId the UUID of the entity affected by the audited action;
     *                 used as a fallback actor lookup key
     * @return the resolved {@link User}, or {@code null} if the actor cannot be determined
     */
    private User resolveActor(UUID entityId) {
        try {
            String email = null;
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                email = SecurityContextHolder.getContext().getAuthentication().getName();
            }

            if (email != null && !email.equals("anonymousUser")) {
                return userRepository.findByEmail(email).orElse(null);
            }

            if (entityId != null) {
                return userRepository.findById(entityId).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Could not resolve audit actor: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Attempts to extract a {@link UUID} entity identifier from a method's return value.
     *
     * <p>Extraction is attempted in the following order:</p>
     * <ol>
     *   <li>If {@code result} is itself a {@link UUID}, it is returned directly.</li>
     *   <li>If {@code result} exposes a {@code getUserId()} method that returns a
     *       {@link UUID} or a parseable {@link String}, that value is returned.</li>
     * </ol>
     *
     * @param result the object returned by the intercepted method
     * @return the extracted {@link UUID}, or {@code null} if extraction is not possible
     */
    private UUID extractUuidFromResult(Object result) {
        if (result == null) return null;
        if (result instanceof UUID uuid) return uuid;

        try {
            Method method = result.getClass().getMethod("getUserId");
            Object val = method.invoke(result);
            if (val instanceof UUID uuid) return uuid;
            if (val instanceof String string) return UUID.fromString(string);
        } catch (Exception e) {
            // Field not found — not an error
        }
        return null;
    }
}
