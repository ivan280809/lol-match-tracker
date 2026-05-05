package com.loltracker.app.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = LolMatchTrackerApplication.class)
@DirtiesContext
class PollLockServiceTest {

  @Autowired private PollLockService pollLockService;
  @Autowired private PollLockRepository pollLockRepository;

  @BeforeEach
  void setUp() {
    pollLockRepository.deleteAll();
  }

  @Test
  void acquirePersistsLockAndRejectsConcurrentLeaseUntilRelease() {
    Optional<PollLease> firstLease = pollLockService.acquire(Duration.ofMinutes(5));
    Optional<PollLease> concurrentLease = pollLockService.acquire(Duration.ofMinutes(5));

    assertTrue(firstLease.isPresent());
    assertTrue(concurrentLease.isEmpty());
    assertEquals(1, pollLockRepository.count());

    PollLockEntity persisted = pollLockRepository.findById("match-poll").orElseThrow();
    assertEquals(firstLease.get().owner(), persisted.getOwner());
    assertNotNull(persisted.getLockedUntil());
    assertTrue(persisted.getLockedUntil().isAfter(persisted.getAcquiredAt()));

    pollLockService.release(firstLease.get());

    Optional<PollLease> nextLease = pollLockService.acquire(Duration.ofMinutes(5));
    assertTrue(nextLease.isPresent());
    assertNotEquals(firstLease.get().owner(), nextLease.get().owner());
  }
}
