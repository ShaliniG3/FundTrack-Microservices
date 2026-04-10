package com.cts.notification_service.service;

import com.cts.notification_service.dto.NotificationRequestDTO;
import com.cts.notification_service.dto.NotificationResponseDTO;
import com.cts.notification_service.dto.SimpleNotificationRequestDTO;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponseDTO sendNotification(NotificationRequestDTO dto);

    NotificationResponseDTO sendSimpleNotification(SimpleNotificationRequestDTO request);

    List<NotificationResponseDTO> getNotificationsByUser(UUID userId);

    NotificationResponseDTO markAsRead(UUID id);

    NotificationResponseDTO updateNotification(UUID id, NotificationRequestDTO dto);

    void deleteNotification(UUID id);

}