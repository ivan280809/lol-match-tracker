package com.loltracker.app.integration.telegram;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class TelegramNotifier {

  private final WebClient.Builder webClientBuilder;

  @Value("${telegram.bot.token}")
  private String botToken;

  @Value("${telegram.chat.id}")
  private String chatId;

  public void send(String message) {
    if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
      throw new IllegalStateException("Telegram is not configured");
    }

    webClientBuilder
        .build()
        .post()
        .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("chat_id", chatId, "text", message))
        .retrieve()
        .bodyToMono(String.class)
        .block();
  }
}

