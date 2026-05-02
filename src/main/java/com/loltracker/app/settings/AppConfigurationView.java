package com.loltracker.app.settings;

public record AppConfigurationView(
    boolean riotApiKeyConfigured,
    RiotRegion riotRegion,
    boolean telegramBotTokenConfigured,
    boolean telegramChatIdConfigured,
    boolean encryptionConfigured) {}
