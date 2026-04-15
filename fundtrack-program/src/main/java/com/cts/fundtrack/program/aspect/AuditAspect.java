package com.cts.fundtrack.program.aspect;

import java.util.UUID;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.AuditClient;
import com.cts.fundtrack.common.dto.AuditRequestDTO;
import com.cts.fundtrack.program.models.Program;

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

    /**
     * This method runs AFTER a method annotated with @Auditable returns successfully.
     * It captures the result to extract the Entity ID.
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditAction(Auditable auditable, Object result) {
        try {
            // 1. Extract User Info from Gateway Headers
            // The Gateway filter we wrote earlier injects these
            String userEmail = request.getHeader("X-User-Email");
            
            // 2. Extract Entity ID from the result
            UUID entityId = null;
            if (result instanceof Program program) {
                entityId = program.getProgramId();
            }

            // 3. Build the Audit DTO
            AuditRequestDTO auditLog = AuditRequestDTO.builder()
                    .action(auditable.action())
                    .entityName(auditable.entityName())
                    .entityId(entityId)
                    // Note: If your Identity service expects a UUID userId, 
                    // you may need to pass the email or have Identity resolve it.
                    .build();

            // 4. Send via Feign to Identity Service
            auditClient.sendAuditLog(auditLog);
            
            log.info("Successfully sent Audit Log for Action: {} on Entity: {}", 
                     auditable.action(), auditable.entityName());

        } catch (Exception e) {
            // We catch errors so that a failed audit doesn't crash the main business transaction
            log.error("NON-CRITICAL ERROR: Failed to propagate audit log. Reason: {}", e.getMessage());
        }
    }
}