package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.repository.AuditRepository;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Default implementation of {@link AuditService} that builds and persists
 * {@link AuditLog} records for user-initiated actions within the Identity Service.
 *
 * <p>For identity-domain events (login, registration, password reset, etc.) the
 * acting user and the affected entity are the same — the user's own UUID is used
 * as the {@code entityId}. The {@code entityName} is supplied by the caller to
 * indicate the domain type (typically {@code EntityType.USER}).</p>
 *
 * <p>The timestamp is set explicitly at creation time via {@link Instant#now()} and
 * also guarded by the {@link AuditLog#onCreate()} JPA lifecycle callback, so every
 * record is guaranteed to carry an accurate UTC timestamp regardless of how it is
 * constructed.</p>
 *
 * @see AuditService
 * @see AuditLog
 * @see AuditRepository
 */
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;

    /**
     * Constructs an {@link AuditLog} record from the supplied parameters and
     * persists it to the {@code audit_logs} table.
     *
     * <p>The user's own UUID ({@link User#getUserId()}) is used as the
     * {@code entityId} because, for authentication events, the user is both
     * the actor and the entity being acted upon.</p>
     *
     * @param user       the {@link User} who performed the action; provides both
     *                   the actor reference and the entity UUID
     * @param action     the {@link ActionType} describing the operation performed
     * @param entityType the {@link EntityType} classifying the domain object
     *                   (typically {@code EntityType.USER} for auth events)
     */
    @Override
    public void logUserAction(User user, ActionType action, EntityType entityType) {
        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityId(user.getUserId()) // For Auth, the entity is usually the user themselves
                .entityName(entityType)
                .timestamp(Instant.now())
                .build();

        auditRepository.save(log);
    }
}
