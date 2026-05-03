package com.cts.fundtrack.common.dto;

import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object used to submit an audit log entry to the Identity Service.
 *
 * <p>Populated by the {@code AuditAspect} in each microservice after a method
 * annotated with {@link com.cts.fundtrack.common.aspect.Auditable} completes
 * successfully. The entry is forwarded to the Identity Service via
 * {@link com.cts.fundtrack.common.client.AuditClient} for persistence.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRequestDTO {

    /** Unique identifier of the user who performed the action. May be {@code null} for system-initiated events. */
    private UUID userId;

    /** The category of action performed (e.g., CREATE, UPDATE, LOGIN). */
    private ActionType action;

    /** Unique identifier of the specific entity instance that was acted upon. May be {@code null} if not applicable. */
    private UUID entityId;

    /**
     * The domain entity type that was the subject of the action (e.g., APPLICATION, PROGRAM).
     * Stored as {@code entityName} to match the audit log schema column naming.
     */
    private EntityType entityName;
}