package com.cts.notification_service.model.enums;

import java.text.MessageFormat;
import java.util.Map;

/**
 * Utility class that acts as a centralized repository for notification message templates.
 * This class maps {@link NotificationCategory} values to human-readable strings.
 * It uses {@link MessageFormat} to allow for dynamic data injection (e.g., Application IDs).
 */
public class NotificationTemplate {

    /**
     * A static mapping of categories to their respective message patterns.
     * The placeholder {0} is typically replaced with an Application ID or Reference Number.
     */
    private static final Map<NotificationCategory, String> TEMPLATES = Map.of(

            NotificationCategory.APPLICATION, "New application submitted for {0}.",
            NotificationCategory.SUBMITTED, "New application submitted for {0}.",
            NotificationCategory.UNDER_REVIEW, "Your application {0} is currently under review.",
            NotificationCategory.APPROVAL, "Congratulations! Your application {0} has been APPROVED.",
            NotificationCategory.REJECTED, "Your application {0} has been REJECTED.",
            NotificationCategory.DISBURSEMENT, "Funds for application {0} have been disbursed.",
            NotificationCategory.COMPLIANCE, "Action Required: Please complete the compliance check for {0}.",
            NotificationCategory.ACCEPTED, "Congratulations! Your application {0} has been accepted for Disbursement."
    );

    /**
     * Retrieves a formatted message based on the provided category and arguments.
     * If a category is not found in the template map, it returns a default generic format.
     *
     * @param category The {@link NotificationCategory} determining which template to use.
     * @param args     The dynamic values (usually UUIDs or Names) to inject into the placeholders.
     * @return A fully formatted string ready for the end-user.
     */
    public static String getMessage(NotificationCategory category, Object... args) {
        // Fallback pattern if a category is missing from the map
        String pattern = TEMPLATES.getOrDefault(category, "Notification: {0}");

        return MessageFormat.format(pattern, args);
    }
}


