package com.cts.fundtrack.identity.aspect;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.repository.AuditRepository;
import com.cts.fundtrack.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditAction(JoinPoint joinPoint, Auditable auditable, Object result) {
        
        UUID entityId = extractUuidFromResult(result);
        User actor = resolveActor(entityId);

        if (actor != null && entityId != null) {
            AuditLog logEntry = AuditLog.builder()
                    .user(actor)
                    .action(auditable.action())
                    .entityId(entityId)
                    .entityName(auditable.entityName())
                    .timestamp(Instant.now())
                    .build();

            auditRepository.save(logEntry);
        }
    }

    private User resolveActor(UUID entityId) {
        String email = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            email = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        if (email != null && !email.equals("anonymousUser")) {
            return userRepository.findByEmail(email).orElse(null);
        }
        
        // Fallback: If no user is logged in (e.g., during Registration), 
        // the user being created is the actor.
        if (entityId != null) {
            return userRepository.findById(entityId).orElse(null);
        }
        return null;
    }

    private UUID extractUuidFromResult(Object result) {
        if (result == null) return null;
        if (result instanceof UUID) return (UUID) result; // Handles resetPassword
        
        try {
            // Handles DTOs returning getUserId()
            Method method = result.getClass().getMethod("getUserId");
            Object val = method.invoke(result);
            if (val instanceof UUID) return (UUID) val;
            if (val instanceof String) return UUID.fromString((String) val);
        } catch (Exception e) {
            // Field not found or reflection error
        }
        return null;
    }
}