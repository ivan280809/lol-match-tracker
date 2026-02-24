package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.domain.ports.out.NotificationPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class MatchNotificationService implements NotificationPort {

  private final WebClient.Builder webClientBuilder;

  @Value("${notification.service.url}")
  private String notificationServiceUrl;

  @Override
  public void notifyMatchHistory(String message) {
    webClientBuilder
        .build()
        .post()
        .uri(notificationServiceUrl)
        .bodyValue(Map.of("text", message))
        .retrieve()
        .bodyToMono(String.class)
        .doOnSuccess(response -> System.out.println("Notification sent successfully: " + response))
        .doOnError(Throwable::printStackTrace)
        .subscribe();
  }
}
