package com.travelplatform.busbooking.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Calls notification-service's internal (non-gateway-routed) API directly via
 * Eureka: lb://notification-service/internal/notifications. Runs @Async so a slow
 * or unavailable notification-service can never add latency to — or fail — a bus
 * ticket booking or cancellation. Any exception here is caught and logged, never
 * rethrown; this is a "best effort" side effect, not part of the booking's
 * transaction or contract. Mirrors hotel-service's NotificationClientImpl exactly.
 */
@Component
public class NotificationClientImpl implements NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientImpl.class);
    private static final String NOTIFICATION_SERVICE_URL = "http://notification-service/internal/notifications";

    @Autowired private RestTemplate loadBalancedRestTemplate;

    @Value("${notification.internal.api-key}")
    private String internalApiKey;

    @Override
    @Async
    public void notify(UUID userId, String recipientEmail, String type, String title, String message, UUID referenceId) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("type", type);
            body.put("title", title);
            body.put("message", message);
            body.put("referenceId", referenceId);
            body.put("sendEmail", recipientEmail != null && !recipientEmail.isBlank());
            body.put("recipientEmail", recipientEmail);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            loadBalancedRestTemplate.postForEntity(NOTIFICATION_SERVICE_URL, new HttpEntity<>(body, headers), Void.class);
        } catch (Exception e) {
            // Swallow deliberately — see class Javadoc. A missing/unreachable
            // notification-service must never affect the booking flow that called this.
            log.warn("Failed to send notification (type={}, userId={}): {}", type, userId, e.getMessage());
        }
    }
}
