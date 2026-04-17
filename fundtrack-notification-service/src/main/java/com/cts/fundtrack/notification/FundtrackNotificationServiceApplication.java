package com.cts.fundtrack.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main entry point for the FundTrack Notification Service.
 *
 * <p>This microservice is responsible for creating, storing, and managing
 * in-platform notifications for all FundTrack users. It supports structured
 * workflow notifications (linked to application lifecycle events) as well as
 * ad-hoc simple notifications.</p>
 *
 * <p>Key capabilities:</p>
 * <ul>
 *   <li>Receives notification requests from other microservices via Feign client
 *       calls to the internal {@code /api/v1/notifications/send} endpoint.</li>
 *   <li>Generates user-facing messages from {@code NotificationTemplate} definitions
 *       when no custom message is supplied.</li>
 *   <li>Tracks notification read/unread status and supports marking as read,
 *       updating, and deleting individual notification records.</li>
 *   <li>Forwards audit log entries for notification events to the Identity Service
 *       via the shared {@code AuditClient} Feign interface.</li>
 *   <li>Registers with Eureka for service discovery by the API Gateway.</li>
 * </ul>
 *
 * <p>The component scan and Feign client scan are both broadened to
 * {@code com.cts.fundtrack} so that shared beans from the {@code fundtrack-common}
 * module (aspects, exception handlers, Feign clients, security filters) are
 * automatically discovered alongside notification-specific components.</p>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.cloud.client.discovery.EnableDiscoveryClient
 * @see org.springframework.cloud.openfeign.EnableFeignClients
 */
@SpringBootApplication(scanBasePackages = "com.cts.fundtrack")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.cts.fundtrack")
public class FundtrackNotificationServiceApplication {

    /**
     * Application bootstrap method.
     *
     * @param args command-line arguments passed to the JVM at startup
     */
    public static void main(String[] args) {
        SpringApplication.run(FundtrackNotificationServiceApplication.class, args);
    }

}