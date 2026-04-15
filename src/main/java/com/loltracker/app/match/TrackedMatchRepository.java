package com.loltracker.app.match;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedMatchRepository extends JpaRepository<TrackedMatchEntity, Long> {

  boolean existsByPlayerIdAndMatchId(Long playerId, String matchId);

  Optional<TrackedMatchEntity> findByPlayerIdAndMatchId(Long playerId, String matchId);

  List<TrackedMatchEntity> findAllByPlayerIdAndNotificationSentFalseOrderByGameEndAtAsc(Long playerId);

  List<TrackedMatchEntity> findTop20ByOrderByGameEndAtDesc();
}
