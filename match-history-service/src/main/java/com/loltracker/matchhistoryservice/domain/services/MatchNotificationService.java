package com.loltracker.matchhistoryservice.domain.services;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class MatchNotificationService {

  private final WebClient.Builder webClientBuilder;

  public void notifyMatchHistory(String gameName, String endOfGameResult, String gameMode) {
    String message =
        String.format(
            "Game Name: %s\nEnd of Game Result: %s\nGame Mode: %s",
            gameName, endOfGameResult, gameMode);

    String url = "http://localhost:8083/notification";
    webClientBuilder
        .build()
        .post()
        .uri(url)
        .bodyValue(Map.of("text", message))
        .retrieve()
        .bodyToMono(String.class)
        .doOnError(Throwable::printStackTrace)
        .subscribe();

    System.out.println("Notification sent: " + message);
  }
}
