package com.travelplatform.notification.enums;

/**
 * Open-ended by design — new event types from any service (hotel, bus, ride,
 * package bookings, future payment events) just add a new constant here. The
 * frontend can map type -> icon/routing without parsing free text.
 */
public enum NotificationType {
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_REJECTED,
    REVIEW_POSTED,
    GENERIC
}
