package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryRecorderTest {

  @Mock private NotificationOutboxRepository notificationOutboxRepository;
  @Mock private TrackedMatchRepository trackedMatchRepository;

  @InjectMocks private NotificationDeliveryRecorder notificationDeliveryRecorder;

  @Test
  void recordSentStoresReceiptAndUpdatesTrackedMatchProjection() {
    TrackedMatchEntity trackedMatch = new TrackedMatchEntity();
    trackedMatch.setNotificationSent(false);

    NotificationOutboxEntity outbox = new NotificationOutboxEntity();
    outbox.setTrackedMatch(trackedMatch);
    outbox.setStatus(NotificationDeliveryStatus.PENDING);

    notificationDeliveryRecorder.recordSent(outbox, 456);

    assertEquals(NotificationDeliveryStatus.SENT, outbox.getStatus());
    assertEquals(456, outbox.getTelegramMessageId());
    assertNotNull(outbox.getSentAt());
    assertTrue(trackedMatch.isNotificationSent());
    assertEquals(outbox.getSentAt(), trackedMatch.getNotificationSentAt());
    verify(notificationOutboxRepository).save(outbox);
    verify(trackedMatchRepository).save(trackedMatch);
  }

  @Test
  void failedDeliveryIsRetriedAfterPersistedBackoff() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    NotificationDeliveryRecorder recorder =
        new NotificationDeliveryRecorder(
            notificationOutboxRepository,
            trackedMatchRepository,
            Clock.fixed(now, ZoneOffset.UTC));
    NotificationOutboxEntity outbox = new NotificationOutboxEntity();
    outbox.setAttemptCount(1);

    recorder.recordFailure(outbox, new IllegalStateException("Telegram returned 429"));

    assertEquals(NotificationDeliveryStatus.FAILED, outbox.getStatus());
    assertEquals(now.plusSeconds(30), outbox.getNextAttemptAt());
    assertEquals("Telegram returned 429", outbox.getLastError());
    verify(notificationOutboxRepository).save(outbox);
  }

  @Test
  void telegram429RetryAfterHeaderControlsTheDurableRetryTime() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    NotificationDeliveryRecorder recorder =
        new NotificationDeliveryRecorder(
            notificationOutboxRepository,
            trackedMatchRepository,
            Clock.fixed(now, ZoneOffset.UTC));
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.RETRY_AFTER, "120");
    RestClientResponseException rateLimit =
        new RestClientResponseException(
            "Telegram rate limited",
            429,
            "Too Many Requests",
            headers,
            new byte[0],
            StandardCharsets.UTF_8);
    NotificationOutboxEntity outbox = new NotificationOutboxEntity();
    outbox.setAttemptCount(1);

    recorder.recordFailure(outbox, rateLimit);

    assertEquals(NotificationDeliveryStatus.FAILED, outbox.getStatus());
    assertEquals(now.plusSeconds(120), outbox.getNextAttemptAt());
    verify(notificationOutboxRepository).save(outbox);
  }

  @Test
  void honorsLongNumericAndHttpDateRetryAfterAndTelegramBodyDelay() {
    Instant now = Instant.parse("2026-10-05T00:00:00Z");
    NotificationDeliveryRecorder recorder = new NotificationDeliveryRecorder(
        notificationOutboxRepository, trackedMatchRepository, Clock.fixed(now, ZoneOffset.UTC));

    NotificationOutboxEntity numeric = new NotificationOutboxEntity();
    numeric.setAttemptCount(1);
    HttpHeaders longHeader = new HttpHeaders();
    longHeader.set(HttpHeaders.RETRY_AFTER, "1800");
    recorder.recordFailure(numeric, response429(longHeader, ""));
    assertEquals(now.plusSeconds(1800), numeric.getNextAttemptAt());

    NotificationOutboxEntity date = new NotificationOutboxEntity();
    date.setAttemptCount(1);
    HttpHeaders dateHeader = new HttpHeaders();
    dateHeader.set(HttpHeaders.RETRY_AFTER, "Mon, 05 Oct 2026 01:00:00 GMT");
    recorder.recordFailure(date, response429(dateHeader, ""));
    assertEquals(now.plusSeconds(3600), date.getNextAttemptAt());

    NotificationOutboxEntity body = new NotificationOutboxEntity();
    body.setAttemptCount(1);
    recorder.recordFailure(
        body,
        response429(
            new HttpHeaders(),
            "{\"ok\":false,\"error_code\":429,\"description\":\"Too Many Requests\","
                + "\"parameters\":{\"retry_after\":7200}}"));
    assertEquals(now.plusSeconds(7200), body.getNextAttemptAt());
  }

  private RestClientResponseException response429(HttpHeaders headers, String body) {
    return new RestClientResponseException(
        "Too Many Requests", 429, "Too Many Requests", headers,
        body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
  }
}
