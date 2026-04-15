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

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class WebNotificationServiceImplementation implements NotificationService {

    private final NotificationRepository repository;

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

    @Override
    public List<NotificationResponseDTO> getNotificationsByUser(UUID userId) {
        log.info("GET /notifications/user/{} - Fetching notifications", userId);

        return repository.findByUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

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