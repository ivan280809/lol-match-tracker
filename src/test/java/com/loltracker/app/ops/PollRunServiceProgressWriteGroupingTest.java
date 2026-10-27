package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.loltracker.app.player.PlayerEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests that PollRunService groups progress writes correctly when the same stage is
 * reported repeatedly.
 */
@ExtendWith(MockitoExtension.class)
class PollRunServiceProgressWriteGroupingTest {
  @Mock
  private PollRunRepository pollRunRepository;

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
    run.setStatus("RUNNING");
    savedEntities = new ArrayList<>();
    when(pollRunRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(run));
    when(pollRunRepository.save(org.mockito.ArgumentMatchers.any(PollRunEntity.class)))
        .thenAnswer(invocation -> {
          PollRunEntity arg = invocation.getArgument(0);
          savedEntities.add(arg);
          return arg;
        });
  }

  @Test
  void groupsOneHundredPlayerProgressUpdatesIntoTwentyWrites() {
    // All updates are for the same stage, so writes should happen on first call and every 5s.
    for (int i = 0; i < 100; i++) {
      PlayerEntity player = new PlayerEntity();
      player.setGameName("player-" + i);
      player.setTagLine("EUW");
      pollRunService.updateProgress(run, player, "Procesando jugadores");
      clock.setInstant(clock.instant().plusSeconds(1));
    }
    // t=0,5,...,95 yields 20 flushes; completion writes the final status separately.
    assertEquals(20, savedEntities.size(), "100 updates should be grouped into 20 writes");
    verify(pollRunRepository, times(20)).findByIdForUpdate(1L);
    verify(pollRunRepository, times(20)).save(run);
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
}
