package com.loltracker.app.match;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatchEntity, Long> {

  @Query("select pm.match.matchId from PlayerMatchEntity pm where pm.player.id = :playerId and pm.match.matchId in :matchIds")
  List<String> findExistingMatchIdsForPlayer(
      @Param("playerId") Long playerId, @Param("matchIds") Collection<String> matchIds);

  boolean existsByPlayerIdAndMatchMatchId(Long playerId, String matchId);

  Optional<PlayerMatchEntity> findByPlayerIdAndMatchMatchId(Long playerId, String matchId);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      order by m.gameEndAt desc, pm.id desc
      """
  )
  List<PlayerMatchEntity> findRecent(Pageable pageable);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      where pm.player.id = :playerId
      order by m.gameEndAt desc, pm.id desc
      """
  )
  List<PlayerMatchEntity> findRecentForPlayer(@Param("playerId") Long playerId, Pageable pageable);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      """
  )
  List<PlayerMatchEntity> findAllForHistorySearch();

  @Query(
      """
      select m.id
      from PlayerMatchEntity pm
      join pm.match m
      group by m.id
      having count(pm.id) > 1
      order by max(m.gameEndAt) desc
      """
  )
  List<Long> findSharedMatchIds(Pageable pageable);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      where m.id in :matchIds
      order by m.gameEndAt desc, pm.id asc
      """
  )
  List<PlayerMatchEntity> findByMatchIdsWithPlayerAndMatch(@Param("matchIds") List<Long> matchIds);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      where m.matchId = :matchId
      order by pm.id asc
      """
  )
  List<PlayerMatchEntity> findByMatchMatchIdWithPlayerAndMatch(@Param("matchId") String matchId);

  /**
   * Filter in PostgreSQL and return IDs without materializing history entities. The service binds
   * escaped contains patterns once, so PostgreSQL can fold constant queue-label predicates and
   * avoids rebuilding the same pattern for every joined row. Cast nullable native parameters to
   * retain PostgreSQL type inference.
   */
  @Query(
      value = """
      SELECT DISTINCT pm.player_id
      FROM player_matches pm
      JOIN players p ON p.id = pm.player_id
      JOIN matches m ON m.id = pm.match_id
      WHERE (CAST(:champion AS text) IS NULL OR
            replace(lower(pm.champion_name), 'ς', 'σ') ILIKE CAST(:champion AS text) )
        AND (CAST(:text AS text) IS NULL OR (
          replace(lower(pm.champion_name), 'ς', 'σ') ILIKE CAST(:text AS text)
          OR replace(lower(TRIM(m.game_mode)), 'ς', 'σ') ILIKE CAST(:text AS text)
          OR replace(lower(m.match_id), 'ς', 'σ') ILIKE CAST(:text AS text)
          OR replace(lower(pm.result::text), 'ς', 'σ') ILIKE CAST(:text AS text)
          OR replace(lower(p.game_name), 'ς', 'σ') ILIKE CAST(:text AS text)
          OR replace(lower(p.tag_line), 'ς', 'σ') ILIKE CAST(:text AS text)
          OR (REPLACE(
                CASE

                  WHEN m.queue_id IN (0,400,420,430,440,450,490,700,830,840,850,1700,1710) THEN NULL
                  WHEN trim(translate(coalesce(m.game_mode, ''), :javaWhitespace,
                                      repeat(' ', length(:javaWhitespace)))) <> ''
                    THEN replace(m.game_mode, '_', ' ')
                  WHEN m.queue_id IS NULL THEN 'Unknown queue'
                  ELSE CONCAT('Queue ', CAST(m.queue_id AS TEXT))
                END,
                'ς',
                'σ'
              ) ILIKE CAST(:text AS text)
          )
          -- Known queue labels
          OR (m.queue_id = 0 AND 'Custom' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 400 AND 'Normal Draft' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 420 AND 'Ranked Solo/Duo' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 430 AND 'Normal Blind' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 440 AND 'Ranked Flex' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 450 AND 'ARAM' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 490 AND 'Quickplay' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 700 AND 'Clash' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 830 AND 'Co-op vs AI Intro' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 840 AND 'Co-op vs AI Beginner' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 850 AND 'Co-op vs AI Intermediate' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 1700 AND 'Arena' ILIKE CAST(:text AS text) )
          OR (m.queue_id = 1710 AND 'Arena' ILIKE CAST(:text AS text) )
        ))
        AND (CAST(:from AS timestamp) IS NULL OR m.game_end_at >= CAST(:from AS timestamp))
        AND (CAST(:to AS timestamp) IS NULL OR m.game_end_at < CAST(:to AS timestamp))
      """,
      nativeQuery = true)
  List<Long> findDistinctPlayerIdsMatchingHistory(
      @Param("champion") String champion,
      @Param("text") String text,
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("javaWhitespace") String javaWhitespace);
}
