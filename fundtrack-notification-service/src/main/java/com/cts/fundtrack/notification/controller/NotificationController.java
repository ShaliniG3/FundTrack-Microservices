package com.cts.fundtrack.notification.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.NotificationResponseDTO;
import com.cts.fundtrack.common.dto.SimpleNotificationRequestDTO;
import com.cts.fundtrack.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller that exposes all notification management endpoints for the
 * FundTrack Notification Service.
 *
 * <p>Supported operations:</p>
 * <ul>
 *   <li><b>Create Workflow Notification</b> — persists a structured, template-driven
 *       notification linked to a specific system event (e.g., application submitted,
 *       decision made).</li>
 *   <li><b>Send Simple Notification</b> — dispatches a free-form ad-hoc message to
 *       a user without requiring an application reference.</li>
 *   <li><b>Get User Notifications</b> — retrieves the full notification history for
 *       a user, ordered newest first.</li>
 *   <li><b>Mark as Read</b> — transitions a notification's status from
 *       {@code UNREAD} to {@code READ}.</li>
 *   <li><b>Update Notification</b> — modifies the message or category of an
 *       existing notification.</li>
 *   <li><b>Delete Notification</b> — permanently removes a notification record.</li>
 *   <li><b>Internal Send</b> — dedicated endpoint for service-to-service
 *       notification delivery via Feign clients.</li>
 * </ul>
 *
 * <p>All endpoints are secured by the gateway-injected identity headers resolved
 * through {@link com.cts.fundtrack.notification.security.GatewayHeaderFilter}.
 * Base path: {@code /api/v1/notifications}.</p>
 *
 * @see NotificationService
 * @see com.cts.fundtrack.notification.service.WebNotificationServiceImplementation
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "Endpoints for triggering automated alerts, manual notifications, and managing user notification history")
public class NotificationController {

    private final NotificationService service;

    /**
     * Creates a structured workflow notification linked to a specific system event.
     *
     * <p>If the request body does not supply a message, a default template message
     * is generated from the {@code category} and {@code applicationId} fields.
     * The notification is persisted with status {@code UNREAD}.</p>
     *
     * @param dto the validated notification payload containing user ID, application ID,
     *            category, and optional custom message
     * @return {@code 201 Created} with the persisted {@link NotificationResponseDTO}
     */
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

    /**
     * Sends a simple, ad-hoc free-form notification to a specific user.
     *
     * <p>The notification is assigned the {@code GENERAL} category and an
     * {@code UNREAD} status. No application reference is required.</p>
     *
     * @param dto the payload containing the target user ID and the custom message text
     * @return {@code 201 Created} with the persisted {@link NotificationResponseDTO}
     */
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

    /**
     * Retrieves all notifications for the specified user, ordered from newest to oldest.
     *
     * @param userId the UUID of the user whose notification history is requested
     * @return {@code 200 OK} with a list of {@link NotificationResponseDTO} objects
     */
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

    /**
     * Marks the specified notification as read by updating its status to {@code READ}.
     *
     * @param notificationId the UUID of the notification to mark as read
     * @return {@code 200 OK} with the updated {@link NotificationResponseDTO}
     * @throws com.cts.fundtrack.common.exceptions.NotificationNotFoundException if no
     *         notification exists for the given ID
     */
    @PatchMapping("/{notificationId}/read")
    @Operation(
            summary = "Mark Notification as Read",
            description = "Updates the status of a notification to READ."
    )
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable UUID notificationId) {
        log.info("PATCH /api/v1/notifications/{}/read - Marking as READ", notificationId);
        return ResponseEntity.ok(service.markAsRead(notificationId));
    }

    /**
     * Updates the message or category of an existing notification.
     *
     * <p>Only non-null, non-blank fields in {@code dto} are applied to the
     * existing record; unchanged fields retain their current values.</p>
     *
     * @param notificationId the UUID of the notification to update
     * @param dto            the update payload; only supplied fields are applied
     * @return {@code 200 OK} with the updated {@link NotificationResponseDTO}
     * @throws com.cts.fundtrack.common.exceptions.NotificationNotFoundException if no
     *         notification exists for the given ID
     */
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

    /**
     * Permanently deletes the specified notification from the data store.
     *
     * @param notificationId the UUID of the notification to delete
     * @return {@code 204 No Content} on successful deletion
     * @throws com.cts.fundtrack.common.exceptions.NotificationNotFoundException if no
     *         notification exists for the given ID
     */
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

    /**
     * Internal endpoint used by other microservices to trigger notifications via
     * Feign client calls.
     *
     * <p>This endpoint mirrors the behaviour of {@link #create(NotificationRequestDTO)}
     * but returns {@code void} so it can be called fire-and-forget by inter-service
     * Feign clients. It is not intended for direct use by the API Gateway or
     * frontend clients.</p>
     *
     * @param request the validated notification payload forwarded by an internal
     *                microservice
     */
    @PostMapping("/send")
    @Operation(
            summary = "Internal Send Notification",
            description = "Endpoint specifically for other microservices to trigger notifications via Feign Client."
    )
    public void receiveInternalNotification(@Valid @RequestBody NotificationRequestDTO request) {
        log.info("Internal Request Received | User: {} | Category: {}",
                 request.getUserId(), request.getCategory());
        // Directing the logic to the service layer
        service.sendNotification(request);
    }
}