package com.loltracker.app.settings;

import java.time.Duration;

public record RuntimeAppConfiguration(
    String riotApiKey,
    RiotRegion riotRegion,
    String telegramBotToken,
    String telegramChatId,
    boolean pollingEnabled,
    boolean pollingManualOnly,
    Duration pollingFixedDelay,
    int pollingMatchWindowSize,
    int pollingPaginationLimit) {

  public RuntimeAppConfiguration(
      String riotApiKey,
      RiotRegion riotRegion,
      String telegramBotToken,
      String telegramChatId) {
    this(riotApiKey, riotRegion, telegramBotToken, telegramChatId, true, false, Duration.ofMinutes(5), 10, 3);
  }
}
