package com.loltracker.notificationservice.domain;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class TelegramService {

  @Value("${telegram.bot.token}")
  private String botToken;

  @Value("${telegram.chat.id}")
  private String chatId;

  private final WebClient.Builder webClientBuilder;

  public void sendText(String text) {
    String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
    webClientBuilder
        .build()
        .post()
        .uri(url)
        .bodyValue(Map.of("chat_id", chatId, "text", text))
        .retrieve()
        .bodyToMono(String.class)
        .doOnError(Throwable::printStackTrace)
        .subscribe();
  }
}
