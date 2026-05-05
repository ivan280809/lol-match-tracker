package com.loltracker.app.integration.telegram;

public interface TelegramNotificationPort {

  TelegramDeliveryReceipt send(String message);
}
