package com.travelplatform.notification.service;

import java.util.UUID;

public interface EmailDispatchService {

    /** Fire-and-forget — runs off the calling thread. See implementation for guarantees. */
    void sendAsync(UUID notificationId, String recipientEmail);
}
