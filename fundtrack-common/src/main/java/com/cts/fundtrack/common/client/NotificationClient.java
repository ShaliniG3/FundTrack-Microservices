package com.cts.fundtrack.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.config.FeignConfig;

/**
 * Feign client for communicating with the {@code fundtrack-notification-service}.
 *
 * <p>This client is used by any microservice that needs to dispatch user-facing
 * notifications (e.g., application status changes, disbursement alerts) without
 * coupling directly to the Notification Service implementation.</p>
 *
 * <p>Header propagation is handled by the shared {@link FeignConfig} interceptor.
 * If the Notification Service is unavailable, {@link NotificationClientFallback}
 * activates and silently discards the notification rather than failing the caller.</p>
 */
@FeignClient(
    name = "fundtrack-notification-service",
    configuration = FeignConfig.class,
    fallback = NotificationClientFallback.class
)
public interface NotificationClient {

    /**
     * Sends a notification to the specified user via the Notification Service.
     *
     * <p>The notification may be templated (based on {@code category}) or use a
     * custom {@code message} if provided directly in the request.</p>
     *
     * @param request the {@link NotificationRequestDTO} containing the recipient,
     *                message content, and notification category
     */
    @PostMapping("/api/v1/notifications/send")
    void sendNotification(@RequestBody NotificationRequestDTO request);
}
