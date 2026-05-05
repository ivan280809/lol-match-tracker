package com.loltracker.app.settings;

public record AppConfigurationView(
    boolean riotApiKeyConfigured,
    RiotRegion riotRegion,
    boolean telegramBotTokenConfigured,
    boolean telegramChatIdConfigured,
    boolean encryptionConfigured,
    String riotApiKeySource,
    String riotRegionSource,
    String telegramBotTokenSource,
    String telegramChatIdSource) {

  public AppConfigurationView(
      boolean riotApiKeyConfigured,
      RiotRegion riotRegion,
      boolean telegramBotTokenConfigured,
      boolean telegramChatIdConfigured,
      boolean encryptionConfigured) {
    this(
        riotApiKeyConfigured,
        riotRegion,
        telegramBotTokenConfigured,
        telegramChatIdConfigured,
        encryptionConfigured,
        riotApiKeyConfigured ? "ENV" : "No configurado",
        "ENV",
        telegramBotTokenConfigured ? "ENV" : "No configurado",
        telegramChatIdConfigured ? "ENV" : "No configurado");
  }

  public boolean telegramConfigured() {
    return telegramBotTokenConfigured && telegramChatIdConfigured;
  }
}
