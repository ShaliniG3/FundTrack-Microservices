package com.cts.fundtrack.common.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.AuditRequestDTO;

/**
 * Circuit breaker fallback for {@link AuditClient}.
 *
 * <p>Activated when the Identity Service (audit endpoint) is unreachable or returns
 * repeated errors. Audit logging is non-critical to the business flow — if the audit
 * service is down, the primary operation still completes. This fallback logs the
 * missed audit entry locally so it can be reviewed.</p>
 */
@Component
public class AuditClientFallback implements AuditClient {

    private static final Logger log = LoggerFactory.getLogger(AuditClientFallback.class);

    /**
     * Logs a warning locally when the Identity Service audit endpoint is unavailable.
     * The audit record is not persisted — the calling operation continues unaffected.
     *
     * @param auditRequest the audit log entry that could not be delivered
     */
    @Override
    public void sendAuditLog(AuditRequestDTO auditRequest) {
        log.warn("[CircuitBreaker] Identity Service (audit) unavailable — audit log dropped: action={}, entity={}, userId={}",
                auditRequest != null ? auditRequest.getAction() : "unknown",
                auditRequest != null ? auditRequest.getEntityName() : "unknown",
                auditRequest != null ? auditRequest.getUserId() : "unknown");
    }
}
