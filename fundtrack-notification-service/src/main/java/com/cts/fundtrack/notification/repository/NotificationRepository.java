package com.cts.fundtrack.notification.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.common.models.enums.NotificationStatus;
import com.cts.fundtrack.notification.models.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // All notifications for a user, newest first
    List<Notification> findByUserIdOrderByCreatedDateDesc(UUID userId);

    // Only unread notifications for a user
    List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status);

    // Count unread — useful for a notification badge on the frontend
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

}