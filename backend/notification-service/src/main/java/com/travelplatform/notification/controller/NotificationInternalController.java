package com.travelplatform.notification.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.notification.dto.CreateNotificationRequest;
import com.travelplatform.notification.dto.NotificationResponse;
import com.travelplatform.notification.entity.Notification;
import com.travelplatform.notification.service.NotificationService;

/**
 * Service-to-service only. Deliberately NOT exposed through the API Gateway (see
 * api-gateway's route config — only /notifications/** is routed, not /internal/**).
 * Callers reach this directly via Eureka (lb://notification-service/internal/notifications)
 * and authenticate with the X-Internal-Api-Key header (see InternalApiKeyFilter),
 * not a user JWT.
 */
@RestController
@RequestMapping("/internal/notifications")
@Validated
@Tag(name = "Notifications (Internal)", description = "Service-to-service notification creation — not gateway-routed")
public class NotificationInternalController {

    @Autowired private NotificationService notificationService;

    @Operation(summary = "Create a notification for a user",
            description = "Called by other services (hotel-service, bus-booking-service, ...) after a " +
                    "domain event. Always creates the in-app notification; email is sent asynchronously " +
                    "and best-effort if sendEmail=true and recipientEmail is provided.")
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@Validated @RequestBody CreateNotificationRequest request) {
        Notification notification = notificationService.createNotification(request);
        return new ResponseEntity<>(NotificationResponse.from(notification), HttpStatus.CREATED);
    }
}
