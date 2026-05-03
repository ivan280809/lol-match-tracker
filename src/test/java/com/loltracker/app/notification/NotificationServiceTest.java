package com.loltracker.app.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.match.TrackedMatchEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private TelegramNotifier telegramNotifier;
  @Mock private NotificationMessageFactory notificationMessageFactory;
  @Mock private NotificationStatsService notificationStatsService;

  @InjectMocks private NotificationService notificationService;

  @Test
  void notifyNewMatchBuildsMessageAndSendsIt() {
    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    NotificationStatsSnapshot stats =
        new NotificationStatsSnapshot(
            "W", 1, "victoria", 1, 0, 1, 0, 1800, "Gold IV 10 LP", "Gold IV 10 LP", 0);
    when(notificationStatsService.buildFor(trackedMatch)).thenReturn(stats);
    when(notificationMessageFactory.build(trackedMatch, stats)).thenReturn("telegram-message");

    notificationService.notifyNewMatch(trackedMatch);

    verify(notificationStatsService).buildFor(trackedMatch);
    verify(notificationMessageFactory).build(trackedMatch, stats);
    verify(telegramNotifier).send("telegram-message");
  }
}
