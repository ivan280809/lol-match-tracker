package com.loltracker.app.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AppConfigurationForm(
    @Size(max = 256) String riotApiKey,
    @NotBlank @Size(max = 32) String riotRegion,
    @Size(max = 256) String telegramBotToken,
    @Size(max = 128) String telegramChatId,
    Boolean pollingEnabled,
    Boolean pollingManualOnly,
    @Pattern(regexp = "^PT\\d+[HMS]$", message = "Usa duracion ISO-8601 simple, por ejemplo PT5M")
        String pollingFixedDelay,
    @Min(1) @Max(100) Integer pollingMatchWindowSize,
    @Min(1) @Max(20) Integer pollingPaginationLimit) {

  public AppConfigurationForm(
      String riotApiKey, String riotRegion, String telegramBotToken, String telegramChatId) {
    this(riotApiKey, riotRegion, telegramBotToken, telegramChatId, true, false, "PT5M", 10, 3);
  }
}
