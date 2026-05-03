package com.loltracker.app.integration.telegram;

import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TelegramNotifier {

  private final WebClient.Builder webClientBuilder;
  private final AppConfigurationService appConfigurationService;

  public void send(String message) {
    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();
    String botToken = configuration.telegramBotToken();
    String chatId = configuration.telegramChatId();
    if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
      throw new IllegalStateException("Telegram is not configured");
    }

    webClientBuilder
        .build()
        .post()
        .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("chat_id", chatId, "text", message, "parse_mode", "HTML"))
        .retrieve()
        .bodyToMono(String.class)
        .block();
  }
}

