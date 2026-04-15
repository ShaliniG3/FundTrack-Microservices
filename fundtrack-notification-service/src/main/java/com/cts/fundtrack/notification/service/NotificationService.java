package com.cts.fundtrack.notification.service;

import java.util.List;
import java.util.UUID;

import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.NotificationResponseDTO;
import com.cts.fundtrack.common.dto.SimpleNotificationRequestDTO;

public interface NotificationService {

    NotificationResponseDTO sendNotification(NotificationRequestDTO dto);

    NotificationResponseDTO sendSimpleNotification(SimpleNotificationRequestDTO request);

    List<NotificationResponseDTO> getNotificationsByUser(UUID userId);

    NotificationResponseDTO markAsRead(UUID id);

    NotificationResponseDTO updateNotification(UUID id, NotificationRequestDTO dto);

    void deleteNotification(UUID id);

}