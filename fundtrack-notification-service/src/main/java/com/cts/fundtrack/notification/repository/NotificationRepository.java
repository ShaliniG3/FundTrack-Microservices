package com.cts.fundtrack.notification.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cts.fundtrack.common.models.enums.NotificationStatus;
import com.cts.fundtrack.notification.models.Notification;

/**
 * Spring Data JPA repository for {@link Notification} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository} and
 * exposes additional query methods tailored to notification retrieval and
 * unread-count use-cases.</p>
 *
 * <p>This repository is the primary data-access layer used by
 * {@link com.cts.fundtrack.notification.service.WebNotificationServiceImplementation}
 * for all notification persistence and query operations.</p>
 *
 * @see Notification
 * @see NotificationStatus
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Retrieves all notifications for the specified user, ordered from newest
     * to oldest by their {@code createdDate}.
     *
     * @param userId the UUID of the user whose notifications are requested
     * @return a list of {@link Notification} entities ordered newest-first;
     *         empty if the user has no notifications
     */
    List<Notification> findByUserIdOrderByCreatedDateDesc(UUID userId);

    /**
     * Retrieves all notifications for the specified user that match the given status.
     *
     * <p>Typically used to fetch only {@code UNREAD} notifications for display
     * in a notification panel or inbox view.</p>
     *
     * @param userId the UUID of the user whose notifications are requested
     * @param status the {@link NotificationStatus} to filter by (e.g., {@code UNREAD})
     * @return a list of matching {@link Notification} entities; empty if none found
     */
    List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status);

    /**
     * Counts the number of notifications for the specified user that match the
     * given status.
     *
     * <p>Useful for rendering an unread-count badge on the frontend without
     * fetching the full notification payloads.</p>
     *
     * @param userId the UUID of the user whose notifications are counted
     * @param status the {@link NotificationStatus} to filter by (e.g., {@code UNREAD})
     * @return the number of notifications matching the user and status criteria
     */
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

}