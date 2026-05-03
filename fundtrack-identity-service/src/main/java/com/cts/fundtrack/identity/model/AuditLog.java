package com.cts.fundtrack.identity.model;

import com.cts.fundtrack.common.models.enums.ActionType;
import com.cts.fundtrack.common.models.enums.EntityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity that represents a single immutable audit log entry in the
 * {@code audit_logs} table.
 *
 * <p>An audit log record is created whenever a security-relevant or
 * business-critical action is performed within the FundTrack platform —
 * such as user registration, login, logout, or password reset. Records
 * originating from external microservices are forwarded to the Identity
 * Service via the internal audit endpoint and stored here alongside
 * identity-service-native entries.</p>
 *
 * <p>Key design decisions:</p>
 * <ul>
 *   <li>The {@code user} association is lazily loaded to avoid unnecessary
 *       joins when only the log metadata is needed.</li>
 *   <li>{@code timestamp} is set by the {@link #onCreate()} lifecycle callback
 *       if the builder does not supply one, ensuring every record is timestamped.</li>
 *   <li>The entity is intentionally not annotated with {@code @Setter} for
 *       {@code auditId} or {@code timestamp} — those fields are managed by JPA
 *       and the lifecycle hook respectively.</li>
 * </ul>
 *
 * @see ActionType
 * @see EntityType
 * @see User
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /**
     * Auto-generated UUID primary key for this audit log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID auditId;

    /**
     * The user who performed the audited action.
     *
     * <p>Lazily fetched to avoid loading the full {@link User} graph when only
     * log metadata is required. Must not be {@code null}.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * The type of action that was performed (e.g., {@code LOGIN}, {@code REGISTER}).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType action;

    /**
     * The UUID of the domain entity that was affected by the action.
     *
     * <p>For user-centric events this is typically the user's own UUID.
     * For events originating from other services it refers to the relevant
     * entity in that service's domain (e.g., a fund application UUID).</p>
     */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    /**
     * The domain type of the affected entity (e.g., {@code USER}, {@code FUND}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_name", nullable = false)
    private EntityType entityName;

    /**
     * The UTC instant at which the audited action occurred.
     *
     * <p>Set automatically by {@link #onCreate()} if not supplied by the builder.
     * The column is marked {@code updatable = false} to preserve the original
     * timestamp and prevent accidental modification.</p>
     */
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    /**
     * JPA lifecycle callback that sets {@link #timestamp} to the current UTC instant
     * immediately before the entity is first persisted, provided no timestamp was
     * already supplied.
     */
    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }
}
