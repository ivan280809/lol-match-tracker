package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.loltracker.app.player.PlayerEntity;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import jakarta.persistence.EntityManager;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = {LolMatchTrackerApplication.class, PollRunServiceProgressComparisonTest.TimeConfig.class},
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.flyway.enabled=false"
    })
@DirtiesContext
class PollRunServiceProgressComparisonTest {
  @Autowired private PollRunService service;
  @Autowired private PollRunRepository repository;
  @Autowired private EntityManager entityManager;
  @Autowired private SessionFactory sessionFactory;
  @Autowired private MutableClock clock;

  @Test
  @Transactional
  void comparesOneHundredActualPerCallUpdatesWithBatchedProgress() {
    ReflectionTestUtils.setField(service, "taskScheduler", null);
    Statistics statistics = sessionFactory.getStatistics();

    clock.reset(Instant.parse("2026-10-05T00:00:00Z"));
    PollRunEntity baseline = service.startRun();
    entityManager.flush();
    statistics.clear();
    for (int i = 0; i < 100; i++) {
      PollRunEntity managed = repository.findByIdForUpdate(baseline.getId()).orElseThrow();
      managed.setCurrentStage("Procesando jugadores");
      managed.setCurrentPlayer("player-" + i + "#EUW");
      repository.save(managed);
      entityManager.flush();
      clock.advance(Duration.ofSeconds(1));
    }
    service.completeRun(baseline, 100, 0, 0, List.of());
    entityManager.flush();
    long baselineUpdates = statistics.getEntityUpdateCount();
    long baselineStatements = statistics.getPrepareStatementCount();
    assertEquals(101, baselineUpdates, "Executed legacy read/update baseline and final run update");
    // baselineStatements is expected to be 202 after 100 select/update pairs and a final update
    assertEquals(202, baselineStatements, "Executed 100 select/update pairs and final update");

    clock.reset(Instant.parse("2026-10-05T00:00:00Z"));
    PollRunEntity batched = service.startRun();
    entityManager.flush();
    statistics.clear();
    for (int i = 0; i < 100; i++) {
      PlayerEntity player = new PlayerEntity();
      player.setGameName("player-" + i);
      player.setTagLine("EUW");
      service.updateProgress(batched, player, "Procesando jugadores");
      entityManager.flush();
      clock.advance(Duration.ofSeconds(1));
    }
    service.completeRun(batched, 100, 0, 0, List.of());
    entityManager.flush();
    long batchedUpdates = statistics.getEntityUpdateCount();
    // Capture statements *after* terminal reads to include the final update and any additional selects
    long batchedStatements = statistics.getPrepareStatementCount();
    System.out.printf("100-update progress measurement: baseline=%d updates/%d statements; batched=%d updates/%d statements%n", baselineUpdates, baselineStatements, batchedUpdates, batchedStatements);
    assertEquals(21, batchedUpdates, "Observed 20 progress flushes and the immediate terminal update");
    // batchedStatements should be 42 after 20 progress flushes (each with a select/update pair) and a final update
    assertEquals(42, batchedStatements, "Observed 20 select/update pairs and final update");
    // Verify terminal state on both baseline and batched runs
    assertEquals("Finalizado", repository.findById(baseline.getId()).orElseThrow().getCurrentStage());
    assertEquals("Finalizado", repository.findById(batched.getId()).orElseThrow().getCurrentStage());
    assertNull(repository.findById(batched.getId()).orElseThrow().getCurrentPlayer(), "Current player should be cleared on terminal state");
    assertEquals("SUCCESS", repository.findById(batched.getId()).orElseThrow().getStatus(), "Terminal status should be SUCCESS");
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    MutableClock comparisonClock() {
      return new MutableClock(Instant.parse("2026-10-05T00:00:00Z"));
    }
  }

  static class MutableClock extends Clock {
    private final AtomicReference<Instant> current;
    MutableClock(Instant initial) { current = new AtomicReference<>(initial); }
    void advance(Duration duration) { current.updateAndGet(value -> value.plus(duration)); }
    void reset(Instant instant) { current.set(instant); }
    @Override public Instant instant() { return current.get(); }
    @Override public ZoneId getZone() { return ZoneOffset.UTC; }
    @Override public Clock withZone(ZoneId zone) { return this; }
  }
}
