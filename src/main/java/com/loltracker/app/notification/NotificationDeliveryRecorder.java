package com.loltracker.app.notification;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryRecorder {

  static final String TELEGRAM_RATE_LIMIT_ERROR_PREFIX = "Telegram rate limit:";

  private final NotificationOutboxRepository notificationOutboxRepository;
  private final TrackedMatchRepository trackedMatchRepository;
  private final Clock clock;

  @Transactional
  public void recordAttempt(NotificationOutboxEntity outbox) {
    outbox.setAttemptCount(outbox.getAttemptCount() + 1);
    outbox.setLastAttemptAt(now());
    outbox.setLastError(null);
    notificationOutboxRepository.save(outbox);
  }

  @Transactional
  public void recordSent(NotificationOutboxEntity outbox, Integer telegramMessageId) {
    Instant now = now();
    outbox.setStatus(NotificationDeliveryStatus.SENT);
    outbox.setTelegramMessageId(telegramMessageId);
    outbox.setSentAt(now);
    outbox.setNextAttemptAt(now);
    outbox.setLastError(null);
    notificationOutboxRepository.save(outbox);

    TrackedMatchEntity trackedMatch = outbox.getTrackedMatch();
    trackedMatch.setNotificationSent(true);
    trackedMatch.setNotificationSentAt(now);
    trackedMatchRepository.save(trackedMatch);
  }

  @Transactional
  public void recordFailure(NotificationOutboxEntity outbox, RuntimeException exception) {
    Instant now = now();
    outbox.setStatus(NotificationDeliveryStatus.FAILED);
    boolean rateLimited = exception instanceof RestClientResponseException response
        && response.getStatusCode().value() == 429;
    // Prefix the error with the rate-limit text only when a 429 was detected.  The
    // combined string must never exceed the database column length of 500
    // characters.  The `shortMessage` helper already limits the raw message to
    // 500 characters, but when we add the prefix the result may exceed that
    // limit.  Truncate the final value to ensure the persistence layer does
    // not trigger a rollback due to a column length violation.
    String rawMessage = shortMessage(exception);
    if (rateLimited) {
      String prefixed = TELEGRAM_RATE_LIMIT_ERROR_PREFIX + " " + rawMessage;
      outbox.setLastError(prefixed.length() > 500 ? prefixed.substring(0, 500) : prefixed);
    } else {
      outbox.setLastError(rawMessage);
    }
    outbox.setNextAttemptAt(now.plus(backoff(outbox.getAttemptCount(), exception, now)));
    notificationOutboxRepository.save(outbox);
    if (rateLimited) {
      notificationOutboxRepository.bulkSetNextAttemptAt(
          List.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED),
          outbox.getNextAttemptAt());
    }
  }

  /**
   * Compute the back‑off duration for a delivery attempt.
   * <p>
   * If the exception is a 429 we try to honour a server supplied
   * {@code Retry-After} header or the {@code parameters.retry_after}
   * field from the JSON body.  Those values are never capped to 900s.
   * For all other errors we apply a linear back‑off capped at 900s.
   */
  private Duration backoff(int attemptCount, RuntimeException exception, Instant now) {
    if (exception instanceof RestClientResponseException response
        && response.getStatusCode().value() == 429) {
      Duration retryAfter = retryAfter(response, now);
      if (retryAfter != null) {
        return retryAfter;
      }
    }
    long seconds = Math.min(900, 30L * Math.max(1, attemptCount));
    return Duration.ofSeconds(seconds);
  }

  private Duration retryAfter(RestClientResponseException response, Instant now) {
    String value = response.getResponseHeaders() == null
        ? null
        : response.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
    if (value != null && !value.isBlank()) {
      Long seconds = parseRetryAfterHeader(value, now);
      if (seconds != null) {
        return Duration.ofSeconds(Math.max(1, seconds));
      }
    }
    // Try JSON body for parameters.retry_after
    try {
      String body = response.getResponseBodyAsString();
      if (body != null) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(body);
        JsonNode param = node.path("parameters").path("retry_after");
        if (!param.isMissingNode() && param.isNumber()) {
          long secs = param.asLong();
          return Duration.ofSeconds(Math.max(1, secs));
        }
      }
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  private Long parseRetryAfterHeader(String value, Instant now) {
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ignored) {
      try {
        Instant retryAt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        return Duration.between(now, retryAt).toSeconds();
      } catch (RuntimeException invalidHeader) {
        return null;
      }
    }
  }

  private String shortMessage(RuntimeException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      message = exception.getClass().getSimpleName();
    }
    return message.substring(0, Math.min(500, message.length()));
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }
}
