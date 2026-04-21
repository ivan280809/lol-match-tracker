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

  @InjectMocks private NotificationService notificationService;

  @Test
  void notifyNewMatchBuildsMessageAndSendsIt() {
    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    when(notificationMessageFactory.build(trackedMatch)).thenReturn("telegram-message");

    notificationService.notifyNewMatch(trackedMatch);

    verify(notificationMessageFactory).build(trackedMatch);
    verify(telegramNotifier).send("telegram-message");
  }
}
