package com.cts.fundtrack.identity.service;

import com.cts.fundtrack.identity.model.AuditLog;
import com.cts.fundtrack.identity.model.User;
import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the contract for programmatic audit logging within
 * the Identity Service.
 *
 * <p>Implementations persist {@link com.cts.fundtrack.identity.model.AuditLog}
 * records that capture security-relevant user actions (e.g., registration, login,
 * password reset) along with the acting user, action type, and affected entity
 * type.</p>
 *
 * <p>Note: most audit entries are created automatically via the
 * {@link com.cts.fundtrack.identity.aspect.AuditAspect} AOP aspect whenever a
 * method annotated with {@code @Auditable} returns successfully. This interface
 * exists for cases where audit entries must be created programmatically rather
 * than through the aspect.</p>
 *
 * @see AuditServiceImpl
 * @see com.cts.fundtrack.identity.model.AuditLog
 */
public interface AuditService {

    /**
     * Records an audit log entry for a user-initiated action.
     *
     * <p>The {@code user} is both the actor performing the action and, for
     * identity-domain events, the affected entity. The {@code entityType}
     * describes the domain object that was acted upon.</p>
     *
     * @param user       the {@link User} who performed the action; must not be {@code null}
     * @param action     the {@link ActionType} describing what was done (e.g.,
     *                   {@code LOGIN}, {@code REGISTER})
     * @param entityType the {@link EntityType} identifying the category of the
     *                   affected domain object (e.g., {@code USER})
     */
    void logUserAction(User user, ActionType action, EntityType entityType);
    List<AuditLog> getUserAuditLogs(UUID userId);
    List<AuditLog> getLogsByAction(com.cts.fundtrack.common.models.enums.ActionType action);
}
