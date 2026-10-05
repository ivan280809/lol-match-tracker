package com.loltracker.app.integration.telegram;

import java.time.Duration;

public interface TelegramNotificationPort {

  TelegramDeliveryReceipt send(String message);

  default TelegramDeliveryReceipt send(String message, Duration timeout) {
    return send(message);
  }
}
