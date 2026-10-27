package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.ops.PollRunEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class PollRunServiceProgressTest {
  @Mock
  private PollRunRepository pollRunRepository;
  @Mock private TaskScheduler taskScheduler;

  private MutableClock clock;

  @InjectMocks
  private PollRunService pollRunService;

  private PollRunEntity run;
  private List<PollRunEntity> savedEntities;

  @BeforeEach
  void setUp() {
    clock = new MutableClock(Instant.parse("2023-01-01T00:00:00Z"));
    pollRunService = new PollRunService(pollRunRepository, clock);
    run = new PollRunEntity();
    run.setId(1L);
    // capture saves
    savedEntities = new ArrayList<>();
    run.setStatus("RUNNING");
    when(pollRunRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(run));
    org.mockito.Mockito.lenient()
        .when(pollRunRepository.save(org.mockito.ArgumentMatchers.any(PollRunEntity.class)))
        .thenAnswer(invocation -> {
          PollRunEntity arg = invocation.getArgument(0);
          savedEntities.add(arg);
          return arg;
        });
  }

  @Test
  void testUpdateProgressFlushesOnlyOnStageChangeOrInterval() {
    // First call – should flush
    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(1, savedEntities.size(), "First update should flush");

    // Second call same stage within 5s – should not flush
    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(1, savedEntities.size(), "Second update within interval should not flush");
    org.mockito.Mockito.verify(pollRunRepository, org.mockito.Mockito.times(1)).findByIdForUpdate(1L);

    // Advancing to the 5-second boundary flushes the latest buffered progress.
    clock.setInstant(clock.instant().plusSeconds(5));
    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(2, savedEntities.size(), "Third update after interval should flush");

    // Different stage – should flush immediately
    pollRunService.updateProgress(run, null, "StageB");
    assertEquals(3, savedEntities.size(), "Update with new stage should flush");
  }

  @Test
  void persistsFinalStateAndClearsBufferedProgressOnCompletion() {
    pollRunService.updateProgress(run, null, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, null, "StageA");

    pollRunService.completeRun(run, 1, 0, 0, List.of());
    assertEquals("Finalizado", run.getCurrentStage());
    assertEquals(2, savedEntities.size());

    pollRunService.updateProgress(run, null, "StageA");
    assertEquals(2, savedEntities.size(), "A terminal run must not accept more progress writes");
    assertEquals("Finalizado", run.getCurrentStage());
  }

  @Test
  void scheduledFlushPersistsLatestPendingProgressAtFiveSecondDeadline() {
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", taskScheduler);
    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    when(taskScheduler.schedule(
            org.mockito.ArgumentMatchers.any(Runnable.class),
            org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenAnswer(
            invocation -> {
              scheduled.set(invocation.getArgument(0));
              return null;
            });
    PlayerEntity first = new PlayerEntity();
    first.setGameName("first");
    first.setTagLine("EUW");
    PlayerEntity latest = new PlayerEntity();
    latest.setGameName("latest");
    latest.setTagLine("EUW");

    pollRunService.updateProgress(run, first, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, latest, "StageA");
    assertEquals(1, savedEntities.size());
    org.mockito.ArgumentCaptor<Instant> deadline =
        org.mockito.ArgumentCaptor.forClass(Instant.class);
    org.mockito.Mockito.verify(taskScheduler)
        .schedule(org.mockito.ArgumentMatchers.any(Runnable.class), deadline.capture());
    assertEquals(Instant.parse("2023-01-01T00:00:04.500Z"), deadline.getValue());

    clock.setInstant(clock.instant().plusSeconds(4));
    scheduled.get().run();

    assertEquals(2, savedEntities.size());
    assertEquals("latest#EUW", run.getCurrentPlayer());
  }

  @Test
  void rollbackDoesNotAdvanceThePersistedProgressCache() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      pollRunService.updateProgress(run, null, "StageA");
      assertEquals(1, savedEntities.size());
      TransactionSynchronizationManager.getSynchronizations()
          .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }

    pollRunService.updateProgress(run, null, "StageA");

    assertEquals(2, savedEntities.size(), "A rolled-back write must be retried instead of treated as persisted");
  }

  @Test
  void scheduledFlushRetriesAfterRepositoryFailureWithoutAnotherProgressUpdate() {
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", taskScheduler);
    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    AtomicReference<Instant> retryDeadline = new AtomicReference<>();
    when(taskScheduler.schedule(
            org.mockito.ArgumentMatchers.any(Runnable.class),
            org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenAnswer(invocation -> {
          scheduled.set(invocation.getArgument(0));
          retryDeadline.set(invocation.getArgument(1));
          return null;
        });
    java.util.concurrent.atomic.AtomicInteger writes = new java.util.concurrent.atomic.AtomicInteger();
    org.mockito.Mockito.doAnswer(invocation -> {
      PollRunEntity value = invocation.getArgument(0);
      if (writes.incrementAndGet() == 2) {
        throw new org.springframework.dao.DataAccessResourceFailureException("fixture database outage");
      }
      savedEntities.add(value);
      return value;
    }).when(pollRunRepository).save(any(PollRunEntity.class));

    pollRunService.updateProgress(run, null, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, null, "StageA");
    Runnable firstScheduledFlush = scheduled.get();

    clock.setInstant(clock.instant().plusSeconds(4));
    firstScheduledFlush.run();
    assertEquals(1, savedEntities.size(), "A failed scheduled write must leave pending progress intact");
    Runnable retry = scheduled.get();
    assertEquals(Instant.parse("2023-01-01T00:00:06Z"), retryDeadline.get());

    clock.setInstant(clock.instant().plusSeconds(1));
    retry.run();

    assertEquals(2, savedEntities.size());
    assertEquals("StageA", run.getCurrentStage());
  }

  @Test
  void scheduledFlushCannotOverwriteTerminalRunState() {
    ReflectionTestUtils.setField(pollRunService, "taskScheduler", taskScheduler);
    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    when(taskScheduler.schedule(
            org.mockito.ArgumentMatchers.any(Runnable.class),
            org.mockito.ArgumentMatchers.any(Instant.class)))
        .thenAnswer(invocation -> {
          scheduled.set(invocation.getArgument(0));
          return null;
        });
    pollRunService.updateProgress(run, null, "StageA");
    clock.setInstant(clock.instant().plusSeconds(1));
    pollRunService.updateProgress(run, null, "StageA");

    run.setStatus("SUCCESS");
    run.setCurrentStage("Finalizado");
    clock.setInstant(clock.instant().plusSeconds(4));
    scheduled.get().run();

    assertEquals("SUCCESS", run.getStatus());
    assertEquals("Finalizado", run.getCurrentStage());
    org.mockito.Mockito.verify(pollRunRepository, org.mockito.Mockito.times(1)).save(run);
  }

  /** Simple mutable clock for testing. */
  private static class MutableClock extends Clock {
    private Instant instant;

    MutableClock(Instant instant) {
      this.instant = instant;
    }

    void setInstant(Instant instant) {
      this.instant = instant;
    }

    @Override
    public Instant instant() {
      return instant;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }
  }
  @Test
  void persistsFailureImmediatelyAndRejectsLaterBufferedProgress() {
    pollRunService.updateProgress(run, null, "StageA");
    pollRunService.failRun(run, 1, 0, 0, "fixture failure");

    assertEquals("ERROR", run.getStatus());
    assertEquals("Error", run.getCurrentStage());
    assertEquals(2, savedEntities.size());
    pollRunService.updateProgress(run, null, "StageB");
    assertEquals(2, savedEntities.size(), "A terminal run must not be overwritten by cached progress");
  }

  @Test
  void persistsRateLimitImmediatelyAndRejectsLaterBufferedProgress() {
    pollRunService.updateProgress(run, null, "StageA");
    pollRunService.rateLimited(run, 1, 0, 0, clock.instant().plusSeconds(30), "fixture rate limit");

    assertEquals("RATE_LIMITED", run.getStatus());
    assertEquals("Pausado por rate limit Riot", run.getCurrentStage());
    assertEquals(2, savedEntities.size());
    pollRunService.updateProgress(run, null, "StageB");
    assertEquals(2, savedEntities.size(), "A terminal run must not be overwritten by cached progress");
  }
}
