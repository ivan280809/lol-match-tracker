package com.loltracker.app.integration.telegram;

import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramNotifier implements TelegramNotificationPort {

  private final RestClient.Builder restClientBuilder;
  private final AppConfigurationService appConfigurationService;
  private final ObjectMapper objectMapper;
  private final OpsMetrics opsMetrics;

  @Value("${app.http.timeout:PT10S}")
  private Duration httpTimeout;

  @Value("${app.http.retry.max-attempts:3}")
  private int httpMaxAttempts;

  @Value("${app.http.retry.backoff:PT1S}")
  private Duration httpRetryBackoff;

  @Override
  public TelegramDeliveryReceipt send(String message) {
    long startedNanos = System.nanoTime();
    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();
    String botToken = configuration.telegramBotToken();
    String chatId = configuration.telegramChatId();
    if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
      IllegalStateException exception = new IllegalStateException("Telegram is not configured");
      recordTelegram("ERROR", "NOT_CONFIGURED", startedNanos);
      throw exception;
    }

    try {
      String body =
          executeWithRetry(
              () ->
                  restClientBuilder
                      .clone()
                      .build()
                      .post()
                      .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                      .contentType(MediaType.APPLICATION_JSON)
                      .body(Map.of("chat_id", chatId, "text", message, "parse_mode", "HTML"))
                      .retrieve()
                      .body(String.class));
      TelegramDeliveryReceipt receipt = parseReceipt(body);
      recordTelegram("OK", "NONE", startedNanos);
      return receipt;
    } catch (RuntimeException e) {
      recordTelegram("ERROR", telegramCategory(e), startedNanos);
      throw e;
    }
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

  private String executeWithRetry(Supplier<String> request) {
    int attempts = Math.max(1, httpMaxAttempts);
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return request.get();
      } catch (RuntimeException e) {
        lastFailure = e;
        if (attempt >= attempts || !isTransientFailure(e)) {
          throw e;
        }
        sleepBeforeRetry(attempt);
      }
    }
    throw lastFailure == null ? new IllegalStateException("Telegram request failed") : lastFailure;
  }

  private boolean isTransientFailure(RuntimeException failure) {
    if (failure instanceof ResourceAccessException) {
      return true;
    }
    if (failure instanceof RestClientResponseException exception) {
      int status = exception.getStatusCode().value();
      return status >= 500 && status <= 599;
    }
    return false;
  }

  private String telegramCategory(RuntimeException failure) {
    if (failure instanceof ResourceAccessException) {
      return "TIMEOUT";
    }
    if (failure instanceof RestClientResponseException exception) {
      int status = exception.getStatusCode().value();
      if (status == 401 || status == 403) {
        return "UNAUTHORIZED";
      }
      if (status == 429) {
        return "RATE_LIMIT";
      }
      if (status >= 500 && status <= 599) {
        return "UNAVAILABLE";
      }
      if (status >= 400 && status <= 499) {
        return "CLIENT_ERROR";
      }
    }
    return "UNKNOWN";
  }

  private void recordTelegram(String status, String category, long startedNanos) {
    if (opsMetrics != null) {
      opsMetrics.recordTelegramRequest("send", status, category, System.nanoTime() - startedNanos);
    }
  }

  private void sleepBeforeRetry(int attempt) {
    try {
      Thread.sleep(httpRetryBackoff.multipliedBy(Math.max(1, attempt)).toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Telegram retry interrupted", e);
    }
  }

}

