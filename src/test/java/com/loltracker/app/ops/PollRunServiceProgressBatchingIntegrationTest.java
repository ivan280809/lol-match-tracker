package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(
    classes = {LolMatchTrackerApplication.class, PollRunServiceProgressBatchingIntegrationTest.TimeConfig.class},
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.flyway.enabled=false"
    })
@DirtiesContext
class PollRunServiceProgressBatchingIntegrationTest {

  @Autowired private PollRunService pollRunService;
  @Autowired private PollRunRepository pollRunRepository;
  @Autowired private SessionFactory sessionFactory;
  @Autowired private MutableClock clock;

  @Test
  void measuresHibernateUpdatesForOneHundredPlayers() {
    // This scenario advances a controlled Clock; the dedicated unit test exercises the scheduler.
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", null);
    PollRunEntity run = pollRunService.startRun();
    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();

    for (int index = 0; index < 100; index++) {
      PlayerEntity player = new PlayerEntity();
      player.setGameName("player-" + index);
      player.setTagLine("EUW");
      pollRunService.updateProgress(run, player, "Procesando jugadores");
      clock.advance(Duration.ofSeconds(1));
    }

    long progressUpdates = statistics.getEntityUpdateCount();
    long progressStatements = statistics.getPrepareStatementCount();
    assertEquals(20, progressUpdates, "Hibernate should issue one progress update per 5-second window");
    assertEquals(40, progressStatements, "Each flush reads and updates once; the unbatched baseline is 200 statements");
    assertEquals(
        "Procesando jugadores",
        pollRunRepository.findById(run.getId()).orElseThrow().getCurrentStage());
    assertEquals(
        "player-95#EUW",
        pollRunRepository.findById(run.getId()).orElseThrow().getCurrentPlayer());

    pollRunService.completeRun(run, 100, 0, 0, List.of());
    assertEquals(21, statistics.getEntityUpdateCount(), "The final poll-run update is persisted immediately");
  }
  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    MutableClock testClock() {
      return new MutableClock(Instant.parse("2026-10-05T00:00:00Z"));
    }
  }

  static class MutableClock extends Clock {
    private final AtomicReference<Instant> current;

    MutableClock(Instant initial) {
      current = new AtomicReference<>(initial);
    }

    void advance(Duration duration) {
      current.updateAndGet(value -> value.plus(duration));
    }

    @Override
    public Instant instant() {
      return current.get();
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  /**
   * Verify that a completed run is protected from subsequent progress updates.
   * The progress cache should be cleared and any scheduled flushes cancelled.
   */
  @Test
  void terminalRunStateIsNotOverwrittenAfterCompletion() {
    // Disable scheduler to make behaviour deterministic
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", null);
    PollRunEntity run = pollRunService.startRun();

    // Perform a few progress updates that will be batched.
    pollRunService.updateProgress(run, null, "Initial stage");
    pollRunService.updateProgress(run, null, "Second stage");

    // Immediately complete the run.
    pollRunService.completeRun(run, 10, 0, 0, List.of());

    // Persisted state should reflect completion.
    PollRunEntity persisted = pollRunRepository.findById(run.getId()).orElseThrow();
    assertEquals("Finalizado", persisted.getCurrentStage());
    assertEquals(null, persisted.getCurrentPlayer());

    // The cache should have been cleared.
    Object cache = ReflectionTestUtils.getField(pollRunService, "progressCache");
    assertEquals(0, ((java.util.Map<?, ?>) cache).size());

    // No new updates should be written after completion.
    long before = sessionFactory.getStatistics().getEntityUpdateCount();
    pollRunService.updateProgress(run, null, "Post‑completion stage");
    long after = sessionFactory.getStatistics().getEntityUpdateCount();
    assertEquals(before, after, "No updates should be persisted after run completion");
  }
}
