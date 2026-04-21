package com.cts.fundtrack.program.aspect;

import java.util.UUID;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.aspect.Auditable;
import com.cts.fundtrack.common.client.AuditClient;
import com.cts.fundtrack.common.dto.AuditRequestDTO;
import com.cts.fundtrack.common.dto.ProgramResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AspectJ aspect that intercepts service methods annotated with
 * {@link Auditable} and publishes audit log entries to the central Audit Service.
 *
 * <p>In the FundTrack system, every significant state-changing operation on a grant
 * funding program (create, update, archive, delete, status change) is annotated with
 * {@code @Auditable}. This aspect listens for the successful return of those methods
 * and asynchronously records who performed the action, what was done, and which entity
 * was affected — without the service layer needing any direct dependency on audit
 * infrastructure.</p>
 *
 * <p>Audit records are dispatched to the Identity/Audit Service via the
 * {@link AuditClient} Feign client. Failures in this cross-service call are caught
 * and logged as non-critical errors so that an audit delivery failure never rolls
 * back or interrupts the main business transaction.</p>
 *
 * <p>User identity is resolved from the {@code X-User-Id} HTTP header injected by
 * the API Gateway, consistent with the broader gateway-propagated security model
 * used across all FundTrack microservices.</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditClient auditClient;
    private final HttpServletRequest request;

    /**
     * After-returning advice that fires when any method annotated with {@link Auditable}
     * completes successfully, capturing the result to extract the affected entity's ID
     * and forwarding an audit record to the Audit Service.
     *
     * <p>Processing steps:</p>
     * <ol>
     *   <li>Reads the {@code X-User-Id} header from the current HTTP request to identify
     *       the acting user. If the header is absent or contains an invalid UUID, the
     *       audit record is still sent with a {@code null} user ID.</li>
     *   <li>Inspects the method's return value: if it is a {@link Program} instance,
     *       the program's UUID is extracted as the {@code entityId}. For void methods
     *       (e.g., {@code archiveProgram}, {@code deleteProgram}), {@code entityId}
     *       remains {@code null}.</li>
     *   <li>Builds an {@link AuditRequestDTO} from the gathered data and dispatches it
     *       to the Audit Service via the {@link AuditClient} Feign client.</li>
     *   <li>Any exception thrown during audit dispatch is caught and logged as a
     *       non-critical error, ensuring audit failures are isolated from the main
     *       business transaction.</li>
     * </ol>
     *
     * @param auditable the {@link Auditable} annotation instance carrying the
     *                  {@code action} ({@link com.cts.fundtrack.common.models.enums.ActionType})
     *                  and {@code entityName} ({@link com.cts.fundtrack.common.models.enums.EntityType})
     *                  metadata declared on the intercepted method.
     * @param result    the value returned by the intercepted method; may be a
     *                  {@link Program} entity (for create/update operations) or
     *                  {@code null} (for void operations such as archive or delete).
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void logAuditAction(Auditable auditable, Object result) {
        try {
            // 1. Extract User Info from Gateway-injected headers
            String userIdStr = request.getHeader("X-User-Id");
            UUID userId = null;
            if (userIdStr != null && !userIdStr.isBlank()) {
                try {
                    userId = UUID.fromString(userIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid UUID in X-User-Id header: {}", userIdStr);
                }
            }

            // 2. Extract Entity ID from the result
            UUID entityId = null;
            if (result instanceof ProgramResponseDTO dto) {
                entityId = dto.getProgramId();
            }

            // 3. Build the Audit DTO
            AuditRequestDTO auditLog = AuditRequestDTO.builder()
                    .userId(userId)
                    .action(auditable.action())
                    .entityName(auditable.entityName())
                    .entityId(entityId)
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
