package com.travelplatform.notification.dto;

import com.travelplatform.notification.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Internal, service-to-service request body — POSTed by other services (hotel-service,
 * bus-booking-service, etc.), never by an end user. See NotificationInternalController.
 *
 * recipientEmail is supplied by the caller rather than looked up here: notification-service
 * intentionally has no User entity and no call to auth-service, so if the caller wants the
 * EMAIL channel it must already know the address (e.g. from the JWT it received, or from its
 * own user_ref cache). Omit it (or leave sendEmail=false) for an in-app-only notification.
 */
public class CreateNotificationRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "message is required")
    private String message;

    private UUID referenceId;

    private boolean sendEmail = false;

    @Email(message = "recipientEmail must be a valid email")
    private String recipientEmail;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }

    public boolean isSendEmail() { return sendEmail; }
    public void setSendEmail(boolean sendEmail) { this.sendEmail = sendEmail; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }
}
