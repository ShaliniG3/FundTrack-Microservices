package com.cts.notification_service.util;

import com.cts.notification_service.dto.NotificationRequestDTO;
import com.cts.notification_service.model.enums.NotificationCategory;
import com.cts.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationHelper {

    private final NotificationService notificationService;

    public void notify(UUID recipientId, NotificationCategory category, UUID applicationId, String message) {
        log.debug("Dispatching Notification Helper | Recipient: {} | Category: {}", recipientId, category);

        try {
            NotificationRequestDTO request = NotificationRequestDTO.builder()
                    .userId(recipientId)
                    .category(category)
                    .applicationId(applicationId)
                    .message(message)
                    .build();

            notificationService.sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to dispatch notification for user {}: {}", recipientId, e.getMessage());
        }
    }
}