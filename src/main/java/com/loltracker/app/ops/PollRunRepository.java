package com.loltracker.app.ops;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRunRepository extends JpaRepository<PollRunEntity, Long> {

  List<PollRunEntity> findTop10ByOrderByStartedAtDesc();

  Optional<PollRunEntity> findFirstByStatusOrderByStartedAtDesc(String status);

  Optional<PollRunEntity> findFirstByRateLimitPausedUntilAfterOrderByRateLimitPausedUntilDesc(Instant now);

  Optional<PollRunEntity> findFirstByFinishedAtIsNotNullOrderByFinishedAtDesc();
}

