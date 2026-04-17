package com.cts.fundtrack.notification.models;

import java.time.Instant;
import java.util.UUID;

import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.common.models.enums.NotificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity representing a single notification record in the {@code notifications} table.
 *
 * <p>A notification is created whenever a significant platform event occurs that a
 * user should be informed about — such as a fund application being submitted,
 * reviewed, approved, or rejected. Notifications can also be created as simple
 * ad-hoc messages without an associated application.</p>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>The {@code userId} column is named {@code userid} in the database to match
 *       the existing schema; the JPA {@code @Column} mapping bridges the naming
 *       discrepancy.</li>
 *   <li>{@code createdDate} is set automatically by the {@link #onCreate()} JPA
 *       lifecycle callback if not provided by the builder, ensuring every record
 *       has an accurate creation timestamp.</li>
 *   <li>The entity is intentionally not linked to a {@code User} foreign key within
 *       this service; only the user's UUID is stored to keep the Notification Service
 *       loosely coupled from the Identity Service.</li>
 * </ul>
 *
 * @see NotificationCategory
 * @see NotificationStatus
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /**
     * Auto-generated UUID primary key uniquely identifying this notification record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", updatable = false, nullable = false)
    private UUID notificationId;

    /**
     * The UUID of the user this notification is intended for.
     *
     * <p>Maps to the {@code userid} column in the database. This is a soft
     * reference only; no foreign key constraint is enforced from this service.</p>
     */
    @Column(name = "userid", nullable = false)
    private UUID userId;

    /**
     * The UUID of the fund application associated with this notification, if any.
     *
     * <p>May be {@code null} for simple ad-hoc notifications that are not linked
     * to a specific application.</p>
     */
    @Column(name = "application_id")
    private UUID applicationId;

    /**
     * The human-readable notification message displayed to the user.
     *
     * <p>If not supplied explicitly, the service layer generates a message from
     * the {@link NotificationCategory} and the application reference using
     * {@code NotificationTemplate.getMessage()}.</p>
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * The category classifying the type of event that triggered this notification
     * (e.g., {@code APPLICATION_SUBMITTED}, {@code DECISION_MADE}, {@code GENERAL}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private NotificationCategory category;

    /**
     * The current read status of the notification ({@code UNREAD} or {@code READ}).
     *
     * <p>New notifications are always created with {@code UNREAD} status. Clients
     * can transition to {@code READ} via the mark-as-read endpoint.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private NotificationStatus status;

    /**
     * The UTC instant at which this notification was created.
     *
     * <p>Set automatically by the {@link #onCreate()} lifecycle callback if not
     * supplied by the builder. Marked {@code updatable = false} to preserve the
     * original creation timestamp.</p>
     */
    @Column(nullable = false, updatable = false)
    private Instant createdDate;

    /**
     * JPA lifecycle callback that sets {@link #createdDate} to the current UTC
     * instant immediately before the entity is first persisted, provided no date
     * was already supplied via the builder.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = Instant.now();
        }
    }
}