package com.travelplatform.notification.service;

import com.travelplatform.notification.dto.CreateNotificationRequest;
import com.travelplatform.notification.entity.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    Notification createNotification(CreateNotificationRequest request);

    List<Notification> getNotificationsForUser(UUID userId);

    long getUnreadCount(UUID userId);

    Notification markAsRead(UUID notificationId, UUID callerId);

    void markAllAsRead(UUID callerId);
}
