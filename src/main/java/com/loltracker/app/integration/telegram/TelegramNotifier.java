package com.loltracker.app.integration.telegram;

import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramNotifier {

  private final WebClient.Builder webClientBuilder;
  private final AppConfigurationService appConfigurationService;
  private final ObjectMapper objectMapper;

  @Value("${app.http.timeout:PT10S}")
  private Duration httpTimeout;

  @Value("${app.http.retry.max-attempts:3}")
  private int httpMaxAttempts;

  @Value("${app.http.retry.backoff:PT1S}")
  private Duration httpRetryBackoff;

  public TelegramDeliveryReceipt send(String message) {
    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();
    String botToken = configuration.telegramBotToken();
    String chatId = configuration.telegramChatId();
    if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
      throw new IllegalStateException("Telegram is not configured");
    }

    String body =
        blockWithRetry(
            webClientBuilder
                .build()
                .post()
                .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("chat_id", chatId, "text", message, "parse_mode", "HTML"))
                .retrieve()
                .bodyToMono(String.class));
    return parseReceipt(body);
  }

  private TelegramDeliveryReceipt parseReceipt(String body) {
    try {
      JsonNode node = objectMapper.readTree(body);
      JsonNode messageId = node.path("result").path("message_id");
      return new TelegramDeliveryReceipt(messageId.isMissingNode() ? null : messageId.asInt());
    } catch (Exception e) {
      log.warn("Telegram accepted sendMessage but the response could not be parsed");
      return new TelegramDeliveryReceipt(null);
    }
  }

  private String blockWithRetry(Mono<String> response) {
    return response
        .timeout(httpTimeout)
        .retryWhen(
            Retry.backoff(Math.max(0, httpMaxAttempts - 1), httpRetryBackoff)
                .filter(this::isTransientFailure)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
        .block();
  }

  private boolean isTransientFailure(Throwable failure) {
    if (failure instanceof TimeoutException || failure instanceof WebClientRequestException) {
      return true;
    }
    if (failure instanceof WebClientResponseException exception) {
      return exception.getStatusCode().is5xxServerError();
    }
    return false;
  }

}

