package com.travelplatform.notification.service;

import com.travelplatform.notification.dto.CreateNotificationRequest;
import com.travelplatform.notification.entity.Notification;
import com.travelplatform.notification.enums.EmailStatus;
import com.travelplatform.notification.exception.NotificationNotFoundException;
import com.travelplatform.notification.exception.UnauthorizedNotificationActionException;
import com.travelplatform.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired private NotificationRepository notificationRepo;
    @Autowired private EmailDispatchService emailDispatchService;

    @Override
    @Transactional
    public Notification createNotification(CreateNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setReferenceId(request.getReferenceId());

        boolean wantsEmail = request.isSendEmail() && request.getRecipientEmail() != null
                && !request.getRecipientEmail().isBlank();
        notification.setEmailStatus(wantsEmail ? EmailStatus.PENDING : EmailStatus.NOT_REQUESTED);

        Notification saved = notificationRepo.save(notification);

        if (wantsEmail) {
            // Fires after this method returns its result to the caller — the in-app
            // notification (and whatever business action triggered it, e.g. a hotel
            // booking) is already committed and safe regardless of what happens next.
            emailDispatchService.sendAsync(saved.getId(), request.getRecipientEmail());
        }

        return saved;
    }

    @Override
    public List<Notification> getNotificationsForUser(UUID userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public Notification markAsRead(UUID notificationId, UUID callerId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getUserId().equals(callerId)) {
            throw new UnauthorizedNotificationActionException(
                    "Only the recipient of this notification can mark it as read.");
        }

        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepo.save(notification);
        }

        return notification;
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID callerId) {
        List<Notification> unread = notificationRepo.findByUserIdAndReadFalseOrderByCreatedAtDesc(callerId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(n -> { n.setRead(true); n.setReadAt(now); });
        notificationRepo.saveAll(unread);
    }
}
