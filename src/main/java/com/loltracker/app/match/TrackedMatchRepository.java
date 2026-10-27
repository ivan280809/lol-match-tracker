package com.loltracker.app.match;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * Return distinct player IDs that match the given history search criteria.  The query
   * mirrors {@link PlayerMatchRepository#findDistinctPlayerIdsMatchingHistory} but operates
   * against the legacy {@code tracked_matches} table.
   */
  @Query(
      """
      select distinct tm.player.id
      from TrackedMatchEntity tm
      where (:champion is null or lower(tm.championName) like cast(:champion as string) ESCAPE '\\')
        and (:text is null or lower(tm.championName) like cast(:text as string) ESCAPE '\\'
            or lower(tm.gameMode) like cast(:text as string) ESCAPE '\\'
            or lower(tm.matchId) like cast(:text as string) ESCAPE '\\'
            or lower(tm.result) like cast(:text as string) ESCAPE '\\'
            or lower(tm.player.gameName) like cast(:text as string) ESCAPE '\\'
            or lower(tm.player.tagLine) like cast(:text as string) ESCAPE '\\'
            or (REPLACE(
                    CASE
                        WHEN tm.queueId IN (0,400,420,430,440,450,490,700,830,840,850,1700,1710) THEN NULL
                        WHEN trim(function('translate', coalesce(tm.gameMode,''), :javaWhitespace,
                                           repeat(' ', length(:javaWhitespace)))) <> ''
                            THEN replace(tm.gameMode,'_',' ')
                        WHEN tm.queueId IS NULL THEN 'Unknown queue'
                        ELSE CONCAT('Queue ', CAST(tm.queueId AS string))
                    END,
                    'ς',
                    'σ'
                ) ILIKE replace(:text,'ς','σ')
            )
            or (tm.queueId = 0 AND 'Custom' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 400 AND 'Normal Draft' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 420 AND 'Ranked Solo/Duo' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 430 AND 'Normal Blind' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 440 AND 'Ranked Flex' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 450 AND 'ARAM' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 490 AND 'Quickplay' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 700 AND 'Clash' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 830 AND 'Co-op vs AI Intro' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 840 AND 'Co-op vs AI Beginner' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 850 AND 'Co-op vs AI Intermediate' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 1700 AND 'Arena' ILIKE replace(:text,'ς','σ') )
            or (tm.queueId = 1710 AND 'Arena' ILIKE replace(:text,'ς','σ') )
            )
        and (:from is null or tm.gameEndAt >= :from)
        and (:to is null or tm.gameEndAt < :to)
      """
  )
  List<Long> findDistinctPlayerIdsMatchingHistory(
      @Param("champion") String champion,
      @Param("text") String text,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("javaWhitespace") String javaWhitespace);
  // New batch existence query
  List<TrackedMatchEntity> findAllByPlayerIdAndMatchIdIn(Long playerId, Collection<String> matchIds);
}
