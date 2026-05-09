package com.loltracker.app.match;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedMatchRepository extends JpaRepository<TrackedMatchEntity, Long> {

  boolean existsByPlayerIdAndMatchId(Long playerId, String matchId);

  Optional<TrackedMatchEntity> findByPlayerIdAndMatchId(Long playerId, String matchId);

  List<TrackedMatchEntity> findAllByPlayerIdAndNotificationSentFalseOrderByGameEndAtAsc(Long playerId);

  List<TrackedMatchEntity> findTop10ByPlayerIdOrderByGameEndAtDesc(Long playerId);

  List<TrackedMatchEntity> findTop20ByPlayerIdAndChampionNameIgnoreCaseOrderByGameEndAtDesc(
      Long playerId, String championName);

  List<TrackedMatchEntity> findTop20ByPlayerIdAndQueueIdOrderByGameEndAtDesc(
      Long playerId, Integer queueId);

  List<TrackedMatchEntity> findTop20ByPlayerIdAndLaneIgnoreCaseOrderByGameEndAtDesc(
      Long playerId, String lane);

  List<TrackedMatchEntity> findTop20ByPlayerIdAndRoleIgnoreCaseOrderByGameEndAtDesc(
      Long playerId, String role);

  List<TrackedMatchEntity> findByPlayerIdOrderByGameEndAtDesc(Long playerId, Pageable pageable);

  @EntityGraph(attributePaths = {"player"})
  List<TrackedMatchEntity> findAllByMatchIdOrderByIdAsc(String matchId);

  List<TrackedMatchEntity>
      findAllByPlayerIdAndGameEndAtGreaterThanEqualAndGameEndAtLessThanOrderByGameEndAtDesc(
          Long playerId, Instant startInclusive, Instant endExclusive);

  List<TrackedMatchEntity> findTop20ByOrderByGameEndAtDesc();

  List<TrackedMatchEntity> findByOrderByGameEndAtDesc(Pageable pageable);
}
