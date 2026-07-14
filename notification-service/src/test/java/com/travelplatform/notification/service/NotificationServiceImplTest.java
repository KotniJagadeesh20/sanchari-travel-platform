package com.travelplatform.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.notification.dto.CreateNotificationRequest;
import com.travelplatform.notification.entity.Notification;
import com.travelplatform.notification.enums.EmailStatus;
import com.travelplatform.notification.enums.NotificationType;
import com.travelplatform.notification.exception.NotificationNotFoundException;
import com.travelplatform.notification.exception.UnauthorizedNotificationActionException;
import com.travelplatform.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepo;
    @Mock private EmailDispatchService emailDispatchService;
    @InjectMocks private NotificationServiceImpl notificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void createNotification_inAppOnly_neverTriggersEmail() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(NotificationType.BOOKING_CONFIRMED);
        request.setTitle("Booking confirmed");
        request.setMessage("Your stay is booked.");
        request.setSendEmail(false);

        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification saved = notificationService.createNotification(request);

        assertEquals(EmailStatus.NOT_REQUESTED, saved.getEmailStatus());
        verifyNoInteractions(emailDispatchService);
    }

    @Test
    void createNotification_withEmail_marksPendingAndDispatchesAsync() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(NotificationType.BOOKING_CONFIRMED);
        request.setTitle("Booking confirmed");
        request.setMessage("Your stay is booked.");
        request.setSendEmail(true);
        request.setRecipientEmail("traveler@example.com");

        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification n = i.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        Notification saved = notificationService.createNotification(request);

        assertEquals(EmailStatus.PENDING, saved.getEmailStatus());
        verify(emailDispatchService).sendAsync(eq(saved.getId()), eq("traveler@example.com"));
    }

    @Test
    void createNotification_sendEmailTrueButNoAddress_skipsEmail() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(NotificationType.GENERIC);
        request.setTitle("Heads up");
        request.setMessage("Something happened.");
        request.setSendEmail(true); // but no recipientEmail supplied

        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification saved = notificationService.createNotification(request);

        assertEquals(EmailStatus.NOT_REQUESTED, saved.getEmailStatus());
        verifyNoInteractions(emailDispatchService);
    }

    @Test
    void markAsRead_throwsUnauthorized_whenCallerIsNotRecipient() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(UUID.randomUUID()); // different user
        notification.setRead(false);

        when(notificationRepo.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThrows(UnauthorizedNotificationActionException.class,
                () -> notificationService.markAsRead(notification.getId(), userId));
    }

    @Test
    void markAsRead_isIdempotent_doesNotOverwriteEarlierReadAt() {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setRead(true);
        var firstReadAt = java.time.LocalDateTime.now().minusDays(1);
        notification.setReadAt(firstReadAt);

        when(notificationRepo.findById(notification.getId())).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notification.getId(), userId);

        assertEquals(firstReadAt, notification.getReadAt(), "Already-read notifications should not update readAt again");
        verify(notificationRepo, never()).save(any());
    }

    @Test
    void markAsRead_throwsNotFound_whenMissing() {
        UUID missingId = UUID.randomUUID();
        when(notificationRepo.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.markAsRead(missingId, userId));
    }
}
