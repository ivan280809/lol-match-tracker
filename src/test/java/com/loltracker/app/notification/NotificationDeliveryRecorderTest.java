package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryRecorderTest {

  @Mock private NotificationOutboxRepository notificationOutboxRepository;
  @Mock private TrackedMatchRepository trackedMatchRepository;

  @InjectMocks private NotificationDeliveryRecorder notificationDeliveryRecorder;

  @Test
  void recordSentStoresReceiptAndUpdatesTrackedMatchProjection() {
    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    trackedMatch.setNotificationSent(false);

    NotificationOutboxEntity outbox = new NotificationOutboxEntity();
    outbox.setTrackedMatch(trackedMatch);
    outbox.setStatus(NotificationDeliveryStatus.PENDING);

    notificationDeliveryRecorder.recordSent(outbox, 456);

    assertEquals(NotificationDeliveryStatus.SENT, outbox.getStatus());
    assertEquals(456, outbox.getTelegramMessageId());
    assertNotNull(outbox.getSentAt());
    assertTrue(trackedMatch.isNotificationSent());
    assertEquals(outbox.getSentAt(), trackedMatch.getNotificationSentAt());
    verify(notificationOutboxRepository).save(outbox);
    verify(trackedMatchRepository).save(trackedMatch);
  }
}
