package com.cts.fundtrack.identity.controller;

import com.cts.fundtrack.common.dto.AuditRequestDTO;
import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.repository.AuditRepository;
import com.cts.fundtrack.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Internal REST controller that accepts audit log entries forwarded by other
 * microservices within the FundTrack platform.
 *
 * <p>External services (e.g., the Fund Service, Application Service) do not own
 * a copy of the {@link User} entity. Instead, they call this endpoint via the
 * shared {@code AuditClient} Feign interface so that their domain events can be
 * recorded in the central {@code audit_logs} table managed by the Identity Service.</p>
 *
 * <p>This controller is intentionally <em>not</em> exposed through the public API
 * Gateway — it is meant for service-to-service communication only and should be
 * protected at the network level.</p>
 *
 * <p>Base path: {@code /api/v1/internal/audit}</p>
 *
 * @see AuditLog
 * @see AuditRequestDTO
 */
@RestController
@RequestMapping("/api/v1/internal/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditInternalController {

    private final AuditRepository auditRepository;
    private final UserRepository userRepository;

    /**
     * Receives an audit log entry from an external microservice and persists it.
     *
     * <p>The calling service provides the {@code userId} of the actor, the
     * {@code action} performed, the affected {@code entityId}, and the
     * {@code entityName} (domain type). This method resolves the full
     * {@link User} object, constructs an {@link AuditLog} record timestamped to
     * the moment of receipt, and saves it to the database.</p>
     *
     * @param dto the audit request payload forwarded by the external microservice;
     *            must contain a valid {@code userId} that exists in the identity database
     * @throws RuntimeException if no {@link User} is found for the supplied {@code userId}
     */
    @PostMapping("/logs")
    public void receiveAuditLog(@RequestBody AuditRequestDTO dto) {
        log.info("Received internal audit log request from external microservice for user: {}", dto.getUserId());

        User actor = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found for auditing"));

        AuditLog logEntry = AuditLog.builder()
                .user(actor)
                .action(dto.getAction())
                .entityId(dto.getEntityId())
                .entityName(dto.getEntityName())
                .timestamp(Instant.now())
                .build();

        auditRepository.save(logEntry);
    }
}
