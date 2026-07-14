package com.travelplatform.busbooking.client;

import java.util.UUID;

public interface NotificationClient {

    /**
     * Fire-and-forget: creates an in-app (and optionally email) notification for a
     * user via notification-service's internal API. Never throws — a failure here
     * must never roll back or fail the booking/cancellation that triggered it. See
     * NotificationClientImpl for the full reasoning.
     */
    void notify(UUID userId, String recipientEmail, String type, String title, String message, UUID referenceId);
}
