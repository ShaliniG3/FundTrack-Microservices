package com.cts.notification_service.controller;

import com.cts.notification_service.dto.NotificationRequestDTO;
import com.cts.notification_service.dto.NotificationResponseDTO;
import com.cts.notification_service.dto.SimpleNotificationRequestDTO;
import com.cts.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "Endpoints for triggering automated alerts, manual notifications, and managing user notification history")
public class NotificationController {

    private final NotificationService service;

    @PostMapping
    @Operation(
            summary = "Create Workflow Notification",
            description = "Triggers a structured notification linked to specific system events.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Notification successfully created"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body")
            }
    )
    public ResponseEntity<NotificationResponseDTO> create(@Valid @RequestBody NotificationRequestDTO dto) {
        log.info("POST /api/v1/notifications - Creating notification");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.sendNotification(dto));
    }

    @PostMapping("/simple")
    @Operation(
            summary = "Send Simple Notification",
            description = "Dispatches a custom ad-hoc message to a specific user."
    )
    public ResponseEntity<NotificationResponseDTO> sendSimpleNotification(
            @RequestBody SimpleNotificationRequestDTO dto) {
        log.info("POST /api/v1/notifications/simple - Sending simple notification");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.sendSimpleNotification(dto));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get User Notification History",
            description = "Retrieves all notifications for a specific user, ordered newest first.",
            parameters = @Parameter(
                    name = "userId",
                    description = "UUID of the target user",
                    example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
            )
    )
    public ResponseEntity<List<NotificationResponseDTO>> getByUser(@PathVariable UUID userId) {
        log.info("GET /api/v1/notifications/user/{} - Fetching notifications", userId);
        return ResponseEntity.ok(service.getNotificationsByUser(userId));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(
            summary = "Mark Notification as Read",
            description = "Updates the status of a notification to READ."
    )
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable UUID notificationId) {
        log.info("PATCH /api/v1/notifications/{}/read - Marking as READ", notificationId);
        return ResponseEntity.ok(service.markAsRead(notificationId));
    }

    @PutMapping("/{notificationId}")
    @Operation(
            summary = "Update Notification Content",
            description = "Updates the message or category of an existing notification."
    )
    public ResponseEntity<NotificationResponseDTO> update(
            @PathVariable UUID notificationId,
            @Valid @RequestBody NotificationRequestDTO dto) {
        log.info("PUT /api/v1/notifications/{} - Updating notification", notificationId);
        return ResponseEntity.ok(service.updateNotification(notificationId, dto));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(
            summary = "Delete Notification",
            description = "Permanently removes a notification record."
    )
    public ResponseEntity<Void> delete(@PathVariable UUID notificationId) {
        log.warn("DELETE /api/v1/notifications/{} - Deleting notification", notificationId);
        service.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }
}