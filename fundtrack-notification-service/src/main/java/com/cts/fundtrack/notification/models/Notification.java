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