package com.cts.fundtrack.common.aspect;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;

/**
 * Marker annotation used to designate service methods whose execution should be
 * recorded in the central audit log.
 *
 * <p>When applied to a method, the {@code AuditAspect} (present in each microservice)
 * intercepts the successful return of that method and asynchronously dispatches an
 * {@link com.cts.fundtrack.common.dto.AuditRequestDTO} to the Identity Service's audit
 * endpoint via {@link com.cts.fundtrack.common.client.AuditClient}.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @Auditable(action = ActionType.CREATE, entityName = EntityType.APPLICATION)
 * public ApplicationResponseDTO submitApplication(ApplicationRequestDTO request) { ... }
 * }</pre>
 *
 * <p>This annotation is retained at runtime ({@link RetentionPolicy#RUNTIME}) so that
 * Spring AOP can inspect it via reflection during pointcut matching.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * The type of action being performed (e.g., {@link ActionType#CREATE},
     * {@link ActionType#READ}, {@link ActionType#STATUS_CHANGE}).
     *
     * @return the action category for the audit entry
     */
    ActionType action();

    /**
     * The domain entity type that is the subject of the action (e.g.,
     * {@link EntityType#APPLICATION}, {@link EntityType#PROGRAM}).
     *
     * @return the entity type for the audit entry
     */
    EntityType entityName();
}