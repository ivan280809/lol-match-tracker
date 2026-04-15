package com.loltracker.app.notification;

import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.match.TrackedMatchEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final TelegramNotifier telegramNotifier;
  private final NotificationMessageFactory notificationMessageFactory;

  public void notifyNewMatch(TrackedMatchEntity trackedMatch) {
    telegramNotifier.send(notificationMessageFactory.build(trackedMatch));
  }
}

