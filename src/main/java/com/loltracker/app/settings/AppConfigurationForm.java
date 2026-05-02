package com.loltracker.app.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AppConfigurationForm(
    @Size(max = 256) String riotApiKey,
    @NotBlank @Size(max = 32) String riotRegion,
    @Size(max = 256) String telegramBotToken,
    @Size(max = 128) String telegramChatId) {}
