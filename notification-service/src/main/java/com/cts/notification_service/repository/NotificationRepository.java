package com.cts.notification_service.repository;

import com.cts.notification_service.model.Notification;
import com.cts.notification_service.model.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // All notifications for a user, newest first
    List<Notification> findByUserIdOrderByCreatedDateDesc(UUID userId);

    // Only unread notifications for a user
    List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status);

    // Count unread — useful for a notification badge on the frontend
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

}