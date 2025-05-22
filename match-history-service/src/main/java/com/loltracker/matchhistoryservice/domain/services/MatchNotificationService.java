package com.loltracker.matchhistoryservice.domain.services;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class MatchNotificationService {

  private final WebClient.Builder webClientBuilder;

  public void notifyMatchHistory(String message) {
    String url = "http://localhost:8083/notification";
    webClientBuilder
        .build()
        .post()
        .uri(url)
        .bodyValue(Map.of("text", message))
        .retrieve()
        .bodyToMono(String.class)
        .doOnSuccess(response -> System.out.println("Notification sent successfully: " + response))
        .doOnError(Throwable::printStackTrace)
        .subscribe();
  }
}
