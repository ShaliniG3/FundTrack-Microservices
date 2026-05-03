package com.cts.fundtrack.notification.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.NotificationResponseDTO;
import com.cts.fundtrack.common.dto.SimpleNotificationRequestDTO;
import com.cts.fundtrack.common.exceptions.NotificationNotFoundException;
import com.cts.fundtrack.common.models.enums.NotificationCategory;
import com.cts.fundtrack.common.models.enums.NotificationStatus;
import com.cts.fundtrack.common.models.enums.NotificationTemplate;
import com.cts.fundtrack.notification.models.Notification;
import com.cts.fundtrack.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Primary implementation of {@link NotificationService} that persists and manages
 * web (in-platform) notifications for the FundTrack Notification Service.
 *
 * <p>All write operations are wrapped in {@code @Transactional} to ensure atomicity.
 * The class is annotated with {@code @Primary} so Spring injects this implementation
 * when multiple {@link NotificationService} beans exist in the context.</p>
 *
 * <p>Message generation strategy:</p>
 * <ul>
 *   <li>For workflow notifications, if the caller supplies a non-blank
 *       {@code message} in the request DTO it is used verbatim; otherwise a
 *       template message is generated from the {@code category} and
 *       {@code applicationId} via {@code NotificationTemplate.getMessage()}.</li>
 *   <li>For simple notifications the caller always supplies the message text
 *       and the {@code GENERAL} category is applied automatically.</li>
 * </ul>
 *
 * <p>All notifications are persisted with an initial status of {@code UNREAD}
 * and a {@code createdDate} set to the current UTC instant at save time.</p>
 *
 * @see NotificationService
 * @see NotificationRepository
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class WebNotificationServiceImplementation implements NotificationService {

    private final NotificationRepository repository;

    /**
     * Creates and persists a structured workflow notification.
     *
     * <p>If no custom message is provided in {@code dto}, a default message is
     * generated using {@code NotificationTemplate.getMessage()} with the
     * notification category and the application ID (or {@code "System"} if no
     * application ID is present). The record is stored with status
     * {@code UNREAD}.</p>
     *
     * @param dto the notification request payload containing user ID, optional
     *            application ID, category, and optional message
     * @return a {@link NotificationResponseDTO} representing the persisted record
     */
    @Override
    @Transactional
    public NotificationResponseDTO sendNotification(NotificationRequestDTO dto) {
        log.info("POST /notifications - Processing notification for User: {}", dto.getUserId());

        String appRef = (dto.getApplicationId() != null)
                ? dto.getApplicationId().toString()
                : "System";

        String message = (dto.getMessage() != null && !dto.getMessage().isBlank())
                ? dto.getMessage()
                : NotificationTemplate.getMessage(dto.getCategory(), appRef);

        Notification notification = Notification.builder()
                .userId(dto.getUserId())
                .applicationId(dto.getApplicationId())
                .category(dto.getCategory())
                .message(message)
                .status(NotificationStatus.UNREAD)
                .createdDate(Instant.now())
                .build();

        Notification saved = repository.save(notification);
        log.info("Notification created with ID: {}", saved.getNotificationId());

        return mapToResponseDTO(saved);
    }

    /**
     * Creates and persists a simple, ad-hoc free-form notification.
     *
     * <p>The notification is assigned the {@code GENERAL} category and
     * {@code UNREAD} status. No application reference is required.</p>
     *
     * @param request the payload containing the target user ID and custom message text
     * @return a {@link NotificationResponseDTO} representing the persisted record
     */
    @Override
    @Transactional
    public NotificationResponseDTO sendSimpleNotification(SimpleNotificationRequestDTO request) {
        log.info("POST /notifications/simple - Sending simple notification to User: {}", request.getUserId());

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .message(request.getMessage())
                .category(NotificationCategory.GENERAL)
                .status(NotificationStatus.UNREAD)
                .createdDate(Instant.now())
                .build();

        return mapToResponseDTO(repository.save(notification));
    }

    /**
     * Retrieves all notifications for the specified user ordered from newest to oldest.
     *
     * @param userId the UUID of the user whose notification history is requested
     * @return a list of {@link NotificationResponseDTO} objects; empty if the user
     *         has no notifications
     */
    @Override
    public List<NotificationResponseDTO> getNotificationsByUser(UUID userId) {
        log.info("GET /notifications/user/{} - Fetching notifications", userId);

        return repository.findByUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    /**
     * Marks the specified notification as read by updating its status to {@code READ}.
     *
     * @param id the UUID of the notification to mark as read
     * @return a {@link NotificationResponseDTO} reflecting the updated {@code READ} status
     * @throws NotificationNotFoundException if no notification exists with the given ID
     */
    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(UUID id) {
        log.info("PATCH /notifications/{}/read - Marking as READ", id);

        Notification n = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Notification {} not found", id);
                    return new NotificationNotFoundException("Notification not found: " + id);
                });

        n.setStatus(NotificationStatus.READ);
        return mapToResponseDTO(repository.save(n));
    }

    /**
     * Updates the message and/or category of an existing notification.
     *
     * <p>Only non-null, non-blank fields in {@code dto} are applied to the
     * existing record; fields not present in the request retain their current values.</p>
     *
     * @param id  the UUID of the notification to update
     * @param dto the update payload; only non-null/non-blank fields are applied
     * @return a {@link NotificationResponseDTO} reflecting the updated state
     * @throws NotificationNotFoundException if no notification exists with the given ID
     */
    @Override
    @Transactional
    public NotificationResponseDTO updateNotification(UUID id, NotificationRequestDTO dto) {
        log.info("PUT /notifications/{} - Updating notification", id);

        Notification existing = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + id));

        if (dto.getMessage() != null && !dto.getMessage().isBlank()) {
            existing.setMessage(dto.getMessage());
        }
        if (dto.getCategory() != null) {
            existing.setCategory(dto.getCategory());
        }

        return mapToResponseDTO(repository.save(existing));
    }

    /**
     * Permanently deletes the specified notification from the data store.
     *
     * @param id the UUID of the notification to delete
     * @throws NotificationNotFoundException if no notification exists with the given ID
     */
    @Override
    @Transactional
    public void deleteNotification(UUID id) {
        log.info("DELETE /notifications/{} - Deleting notification", id);

        if (!repository.existsById(id)) {
            log.error("Delete failed: Notification {} not found", id);
            throw new NotificationNotFoundException("Delete failed: Notification not found: " + id);
        }

        repository.deleteById(id);
        log.info("Notification {} deleted successfully", id);
    }

    /**
     * Maps a {@link Notification} entity to its corresponding
     * {@link NotificationResponseDTO} response representation.
     *
     * @param entity the {@link Notification} entity to convert
     * @return a populated {@link NotificationResponseDTO}
     */
    private NotificationResponseDTO mapToResponseDTO(Notification entity) {
        return NotificationResponseDTO.builder()
                .notificationId(entity.getNotificationId())
                .userId(entity.getUserId())
                .applicationId(entity.getApplicationId())
                .message(entity.getMessage())
                .category(entity.getCategory())
                .status(entity.getStatus())
                .createdDate(entity.getCreatedDate())
                .build();
    }
}