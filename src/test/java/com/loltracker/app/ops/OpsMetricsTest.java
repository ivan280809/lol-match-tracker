package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loltracker.app.notification.NotificationDeliveryStatus;
import com.loltracker.app.notification.NotificationOutboxRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OpsMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final NotificationOutboxRepository outboxRepository =
      mock(NotificationOutboxRepository.class);
  private final PollRunRepository pollRunRepository = mock(PollRunRepository.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);
  private final OpsMetrics opsMetrics =
      new OpsMetrics(registry, outboxRepository, pollRunRepository, clock);

  @Test
  void registersLowCardinalityOperationalMeters() {
    when(outboxRepository.countByStatus(NotificationDeliveryStatus.PENDING)).thenReturn(3L);
    when(outboxRepository.countByStatus(NotificationDeliveryStatus.FAILED)).thenReturn(1L);
    when(pollRunRepository.countByRateLimitPausedUntilAfter(clock.instant())).thenReturn(1L);

    opsMetrics.registerGauges();
    opsMetrics.recordPollRun("RATE_LIMITED", Duration.ofSeconds(2).toNanos());
    opsMetrics.recordRiotRequest("match_detail", "ERROR", "RATE_LIMIT", Duration.ofMillis(25).toNanos());
    opsMetrics.recordTelegramRequest("send", "OK", "NONE", Duration.ofMillis(5).toNanos());
    opsMetrics.recordOutboxEnqueue("created");
    opsMetrics.recordOutboxDispatch("failed");
    opsMetrics.recordRateLimitPause(Duration.ofSeconds(120));

    assertEquals(
        1.0,
        registry
            .counter("loltracker.poll.runs", "status", "rate_limited")
            .count());
    assertEquals(
        3.0,
        registry
            .find("loltracker.outbox.notifications")
            .tag("status", "pending")
            .gauge()
            .value());
    assertEquals(
        1,
        registry
            .find("loltracker.riot.requests")
            .tag("operation", "match_detail")
            .tag("status", "error")
            .tag("category", "rate_limit")
            .timer()
            .count());
    assertEquals(
        1,
        registry
            .find("loltracker.telegram.requests")
            .tag("operation", "send")
            .tag("status", "ok")
            .tag("category", "none")
            .timer()
            .count());
    assertEquals(1.0, registry.counter("loltracker.outbox.enqueue", "result", "created").count());
    assertEquals(1.0, registry.counter("loltracker.outbox.dispatch", "result", "failed").count());
    assertEquals(1.0, registry.counter("loltracker.rate_limit.pauses", "integration", "riot").count());
  }
}
