package com.loltracker.app.ops;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PollRunRepository extends JpaRepository<PollRunEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select run from PollRunEntity run where run.id = :id")
  Optional<PollRunEntity> findByIdForUpdate(@Param("id") Long id);

  List<PollRunEntity> findTop10ByOrderByStartedAtDesc();

  Optional<PollRunEntity> findFirstByStatusOrderByStartedAtDesc(String status);

  Optional<PollRunEntity> findFirstByRateLimitPausedUntilAfterOrderByRateLimitPausedUntilDesc(Instant now);

  long countByRateLimitPausedUntilAfter(Instant now);

  List<PollRunEntity> findTop5ByRateLimitPausedUntilIsNotNullOrderByStartedAtDesc();

  Optional<PollRunEntity> findFirstByFinishedAtIsNotNullOrderByFinishedAtDesc();
}

