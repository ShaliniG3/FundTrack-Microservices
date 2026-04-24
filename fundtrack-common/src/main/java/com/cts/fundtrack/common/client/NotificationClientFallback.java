package com.cts.fundtrack.common.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cts.fundtrack.common.dto.NotificationRequestDTO;
import com.cts.fundtrack.common.dto.SimpleNotificationRequestDTO;

/**
 * Circuit breaker fallback for {@link NotificationClient}.
 *
 * <p>Activated when the Notification Service is unreachable or returns repeated errors.
 * Notification delivery failures are non-critical — the underlying business operation
 * (application submission, status update, etc.) has already completed. This fallback
 * logs the missed notification so it can be investigated or retried manually.</p>
 */
@Component
public class NotificationClientFallback implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Override
    public void sendNotification(NotificationRequestDTO request) {
        log.warn("[CircuitBreaker] Notification Service unavailable — notification dropped: category={}, userId={}",
                request != null ? request.getCategory() : "unknown",
                request != null ? request.getUserId() : "unknown");
    }

    @Override
    public void sendSimpleNotification(SimpleNotificationRequestDTO request) {
        log.warn("[CircuitBreaker] Notification Service unavailable — simple notification dropped: userId={}",
                request != null ? request.getUserId() : "unknown");
    }
}
