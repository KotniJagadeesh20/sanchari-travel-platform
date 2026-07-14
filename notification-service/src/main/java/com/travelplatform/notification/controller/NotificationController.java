package com.travelplatform.notification.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.travelplatform.notification.dto.NotificationResponse;
import com.travelplatform.notification.dto.UnreadCountResponse;
import com.travelplatform.notification.entity.Notification;
import com.travelplatform.notification.service.NotificationService;

/** End-user notification inbox — reachable through the API Gateway, JWT-authenticated. */
@RestController
@RequestMapping("/notifications")
@Validated
@Tag(name = "Notifications", description = "The end user's own notification inbox")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    @Autowired private NotificationService notificationService;

    @Operation(summary = "List my notifications", description = "Newest first. Includes both read and unread.")
    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        List<NotificationResponse> notifications = notificationService
                .getNotificationsForUser(UUID.fromString(userIdStr))
                .stream().map(NotificationResponse::from).collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Unread notification count", description = "For a badge/counter in the UI.")
    @GetMapping("/me/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        long count = notificationService.getUnreadCount(UUID.fromString(userIdStr));
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @Operation(summary = "Mark one notification as read")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable UUID notificationId,
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        Notification notification = notificationService.markAsRead(notificationId, UUID.fromString(userIdStr));
        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    @Operation(summary = "Mark all my notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(hidden = true) @RequestHeader("X-Authenticated-User-Id") String userIdStr) {

        notificationService.markAllAsRead(UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }
}
