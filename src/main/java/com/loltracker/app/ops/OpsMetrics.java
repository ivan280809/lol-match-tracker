package com.loltracker.app.ops;

import com.loltracker.app.notification.NotificationDeliveryStatus;
import com.loltracker.app.notification.NotificationOutboxRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OpsMetrics {

  private final MeterRegistry meterRegistry;
  private final NotificationOutboxRepository notificationOutboxRepository;
  private final PollRunRepository pollRunRepository;
  private final Clock clock;

  @PostConstruct
  void registerGauges() {
    Gauge.builder(
            "loltracker.outbox.notifications",
            notificationOutboxRepository,
            repository -> repository.countByStatus(NotificationDeliveryStatus.PENDING))
        .description("Notification outbox items by status")
        .tag("status", "pending")
        .register(meterRegistry);
    Gauge.builder(
            "loltracker.outbox.notifications",
            notificationOutboxRepository,
            repository -> repository.countByStatus(NotificationDeliveryStatus.FAILED))
        .description("Notification outbox items by status")
        .tag("status", "failed")
        .register(meterRegistry);
    Gauge.builder(
            "loltracker.rate_limit.paused",
            pollRunRepository,
            repository -> repository.countByRateLimitPausedUntilAfter(clock.instant()))
        .description("Active Riot rate-limit pause count")
        .tag("integration", "riot")
        .register(meterRegistry);
  }

  public void recordPollRun(String status, long durationNanos) {
    String safeStatus = tag(status);
    Counter.builder("loltracker.poll.runs")
        .description("Polling runs by final status")
        .tag("status", safeStatus)
        .register(meterRegistry)
        .increment();
    Timer.builder("loltracker.poll.duration")
        .description("Polling run duration")
        .tag("status", safeStatus)
        .register(meterRegistry)
        .record(Math.max(0, durationNanos), TimeUnit.NANOSECONDS);
  }

  public void recordRateLimitPause(Duration duration) {
    Counter.builder("loltracker.rate_limit.pauses")
        .description("Rate-limit pauses by integration")
        .tag("integration", "riot")
        .register(meterRegistry)
        .increment();
    DistributionSummary.builder("loltracker.rate_limit.pause.seconds")
        .description("Rate-limit pause duration in seconds")
        .tag("integration", "riot")
        .register(meterRegistry)
        .record(Math.max(0, duration == null ? 0 : duration.toSeconds()));
  }

  public void recordRiotRequest(
      String operation, String status, String category, long durationNanos) {
    Timer.builder("loltracker.riot.requests")
        .description("Riot adapter request duration")
        .tag("operation", tag(operation))
        .tag("status", tag(status))
        .tag("category", tag(category))
        .register(meterRegistry)
        .record(Math.max(0, durationNanos), TimeUnit.NANOSECONDS);
  }

  public void recordTelegramRequest(String operation, String status, String category, long durationNanos) {
    Timer.builder("loltracker.telegram.requests")
        .description("Telegram adapter request duration")
        .tag("operation", tag(operation))
        .tag("status", tag(status))
        .tag("category", tag(category))
        .register(meterRegistry)
        .record(Math.max(0, durationNanos), TimeUnit.NANOSECONDS);
  }

  public void recordOutboxEnqueue(String result) {
    Counter.builder("loltracker.outbox.enqueue")
        .description("Notification outbox enqueue results")
        .tag("result", tag(result))
        .register(meterRegistry)
        .increment();
  }

  public void recordOutboxDispatch(String result) {
    Counter.builder("loltracker.outbox.dispatch")
        .description("Notification outbox dispatch results")
        .tag("result", tag(result))
        .register(meterRegistry)
        .increment();
  }

  private String tag(String value) {
    if (value == null || value.isBlank()) {
      return "unknown";
    }
    return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
  }
}
