package com.loltracker.app.ops;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PollLockService {

  private static final String POLL_LOCK_NAME = "match-poll";

  private final PollLockRepository pollLockRepository;
  private final Clock clock;

  @Transactional
  public Optional<PollLease> acquire(Duration leaseDuration) {
    PollLockEntity lock = findOrCreateLock();
    Instant now = now();
    if (lock.getLockedUntil() != null && lock.getLockedUntil().isAfter(now)) {
      return Optional.empty();
    }
    String owner = UUID.randomUUID().toString();
    Instant lockedUntil = now.plus(leaseDuration);
    lock.setOwner(owner);
    lock.setAcquiredAt(now);
    lock.setLockedUntil(lockedUntil);
    lock.setUpdatedAt(now);
    pollLockRepository.save(lock);
    return Optional.of(new PollLease(owner, lockedUntil));
  }

  @Transactional
  public void release(PollLease lease) {
    if (lease == null) {
      return;
    }
    pollLockRepository
        .findByNameForUpdate(POLL_LOCK_NAME)
        .filter(lock -> lease.owner().equals(lock.getOwner()))
        .ifPresent(
            lock -> {
              lock.setOwner(null);
              lock.setLockedUntil(null);
              lock.setUpdatedAt(now());
              pollLockRepository.save(lock);
            });
  }

  private PollLockEntity findOrCreateLock() {
    return pollLockRepository
        .findByNameForUpdate(POLL_LOCK_NAME)
        .orElseGet(
            () -> {
              try {
                PollLockEntity lock = new PollLockEntity();
                lock.setName(POLL_LOCK_NAME);
                lock.setUpdatedAt(now());
                return pollLockRepository.saveAndFlush(lock);
              } catch (DataIntegrityViolationException e) {
                return pollLockRepository.findByNameForUpdate(POLL_LOCK_NAME).orElseThrow(() -> e);
              }
            });
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }
}
