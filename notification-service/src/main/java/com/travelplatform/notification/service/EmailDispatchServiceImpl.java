package com.travelplatform.notification.service;

import com.travelplatform.notification.entity.Notification;
import com.travelplatform.notification.enums.EmailStatus;
import com.travelplatform.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sends the email side of a notification, off the request thread, and never lets a
 * failure here propagate back to whatever created the notification in the first
 * place. A booking (or anything else) that triggers a notification must succeed or
 * fail on its own merits — email delivery is a bonus, not a dependency.
 *
 * Each attempt reloads the Notification by ID in its own transaction rather than
 * reusing the entity passed at creation time, since @Async hands off to a different
 * thread (and the original persistence context is gone by the time this runs).
 */
@Service
public class EmailDispatchServiceImpl implements EmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchServiceImpl.class);

    @Autowired private JavaMailSender mailSender;
    @Autowired private NotificationRepository notificationRepo;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.from}")
    private String fromAddress;

    @Override
    @Async
    public void sendAsync(UUID notificationId, String recipientEmail) {
        Notification notification = notificationRepo.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Notification {} disappeared before email could be sent", notificationId);
            return;
        }

        if (!emailEnabled) {
            log.info("Email channel disabled (notification.email.enabled=false) — skipping {}", notificationId);
            notification.setEmailStatus(EmailStatus.FAILED);
            notificationRepo.save(notification);
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(recipientEmail);
            mail.setSubject(notification.getTitle());
            mail.setText(notification.getMessage());
            mailSender.send(mail);

            notification.setEmailStatus(EmailStatus.SENT);
        } catch (Exception e) {
            // Deliberately broad: any SMTP/config/network failure here should degrade
            // to "email failed," not bubble up and crash an async thread silently.
            log.error("Failed to send email for notification {}: {}", notificationId, e.getMessage());
            notification.setEmailStatus(EmailStatus.FAILED);
        }

        notificationRepo.save(notification);
    }
}
