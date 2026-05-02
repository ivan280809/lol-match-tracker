package com.loltracker.app.settings;

public record RuntimeAppConfiguration(
    String riotApiKey,
    RiotRegion riotRegion,
    String telegramBotToken,
    String telegramChatId) {}
