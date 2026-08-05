package com.travelplatform.notification.enums;

/**
 * Tracks the email channel independently of the in-app notification itself.
 * A Notification always exists in-app the moment it's created; the email side
 * effect is optional and asynchronous, and can fail without affecting the
 * in-app record at all.
 */
public enum EmailStatus {
    /** Caller didn't request email for this notification — in-app only. */
    NOT_REQUESTED,
    /** Email requested, dispatch queued/running. */
    PENDING,
    SENT,
    FAILED
}
