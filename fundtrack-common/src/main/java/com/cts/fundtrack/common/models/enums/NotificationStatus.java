package com.cts.fundtrack.common.models.enums;

/**
 * Represents the read/unread state of a user notification in the FundTrack system.
 *
 * <p>Used by the Notification Service to track whether a recipient has viewed a
 * notification, enabling unread-count badges and inbox filtering in the UI.</p>
 */
public enum NotificationStatus {

    /** The notification has been viewed by the recipient. */
    READ,

    /** The notification has been delivered but not yet viewed by the recipient. */
    UNREAD
}