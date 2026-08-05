package com.travelplatform.notification.dto;

import com.travelplatform.notification.entity.Notification;
import com.travelplatform.notification.enums.EmailStatus;
import com.travelplatform.notification.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationResponse {

    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private UUID referenceId;
    private Boolean read;
    private LocalDateTime readAt;
    private EmailStatus emailStatus;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.type = n.getType();
        r.title = n.getTitle();
        r.message = n.getMessage();
        r.referenceId = n.getReferenceId();
        r.read = n.getRead();
        r.readAt = n.getReadAt();
        r.emailStatus = n.getEmailStatus();
        r.createdAt = n.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public UUID getReferenceId() { return referenceId; }
    public Boolean getRead() { return read; }
    public LocalDateTime getReadAt() { return readAt; }
    public EmailStatus getEmailStatus() { return emailStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
