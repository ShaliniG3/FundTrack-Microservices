package com.cts.notification_service.model;

import com.cts.notification_service.model.enums.NotificationCategory;
import com.cts.notification_service.model.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", updatable = false, nullable = false)
    private UUID notificationId;

    @Column(name = "userid", nullable = false)   // matches existing DB column exactly
    private UUID userId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private NotificationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdDate;

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = Instant.now();
        }
    }
}