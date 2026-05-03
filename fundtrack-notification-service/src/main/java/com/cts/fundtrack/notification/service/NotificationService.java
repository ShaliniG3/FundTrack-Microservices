package com.cts.fundtrack.notification.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.NotificationResponseDTO;
import com.cts.fundtrack.common.dto.SimpleNotificationRequestDTO;

/**
 * Service interface defining the contract for all notification operations in the
 * FundTrack Notification Service.
 *
 * <p>Implementations are responsible for:</p>
 * <ul>
 *   <li>Creating and persisting structured workflow notifications driven by
 *       platform events (application submitted, decision made, etc.).</li>
 *   <li>Creating simple, ad-hoc free-form notifications for a specific user.</li>
 *   <li>Retrieving a user's full notification history.</li>
 *   <li>Marking notifications as read.</li>
 *   <li>Updating notification content (message or category).</li>
 *   <li>Permanently deleting individual notification records.</li>
 * </ul>
 *
 * <p>The primary implementation is
 * {@link WebNotificationServiceImplementation}, which persists records via
 * {@link com.cts.fundtrack.notification.repository.NotificationRepository}.</p>
 *
 * @see WebNotificationServiceImplementation
 */
public interface NotificationService {

    /**
     * Creates and persists a structured workflow notification linked to a specific
     * platform event.
     *
     * <p>If the request does not supply a custom message, a default message is
     * generated from the {@code category} and {@code applicationId} using
     * {@code NotificationTemplate.getMessage()}. The notification is persisted
     * with status {@code UNREAD}.</p>
     *
     * @param dto the notification request payload containing user ID, optional
     *            application ID, category, and optional message
     * @return a {@link NotificationResponseDTO} representing the persisted record
     */
    NotificationResponseDTO sendNotification(NotificationRequestDTO dto);

    /**
     * Creates and persists a simple, ad-hoc free-form notification for a user.
     *
     * <p>The notification is assigned the {@code GENERAL} category and an
     * {@code UNREAD} status. No application reference is required.</p>
     *
     * @param request the payload containing the target user ID and the message text
     * @return a {@link NotificationResponseDTO} representing the persisted record
     */
    NotificationResponseDTO sendSimpleNotification(SimpleNotificationRequestDTO request);

    /**
     * Retrieves all notifications for the specified user, ordered from newest
     * to oldest.
     *
     * @param userId the UUID of the user whose notification history is requested
     * @return a list of {@link NotificationResponseDTO} objects; empty if the user
     *         has no notifications
     */
    List<NotificationResponseDTO> getNotificationsByUser(UUID userId);

    /**
     * Marks the specified notification as read by setting its status to {@code READ}.
     *
     * @param id the UUID of the notification to mark as read
     * @return a {@link NotificationResponseDTO} reflecting the updated state
     * @throws com.cts.fundtrack.common.exceptions.NotificationNotFoundException if no
     *         notification exists with the given ID
     */
    NotificationResponseDTO markAsRead(UUID id);

    /**
     * Updates the message and/or category of an existing notification.
     *
     * <p>Only non-null, non-blank fields present in {@code dto} are applied;
     * unchanged fields retain their current values.</p>
     *
     * @param id  the UUID of the notification to update
     * @param dto the update payload; only supplied fields are applied
     * @return a {@link NotificationResponseDTO} reflecting the updated state
     * @throws com.cts.fundtrack.common.exceptions.NotificationNotFoundException if no
     *         notification exists with the given ID
     */
    NotificationResponseDTO updateNotification(UUID id, NotificationRequestDTO dto);

    /**
     * Permanently deletes the specified notification from the data store.
     *
     * @param id the UUID of the notification to delete
     * @throws com.cts.fundtrack.common.exceptions.NotificationNotFoundException if no
     *         notification exists with the given ID
     */
    void deleteNotification(UUID id);

}