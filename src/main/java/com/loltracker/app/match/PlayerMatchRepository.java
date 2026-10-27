package com.loltracker.app.match;

import java.util.Optional;
import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatchEntity, Long> {

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
   * Return distinct player ids that match the given history search criteria.
   * The query implements the same semantics as {@link MatchReadModelService#findPlayerIdsMatchingHistory}
   * but performs the filtering inside the database.
   */
  /**
   * Find distinct player ids that match the history search criteria. The query is written in native
   * SQL to avoid PostgreSQL type inference issues when binding parameters of null values. The
   * parameters are escaped in {@link MatchReadModelService#escapeLike(String)} before being passed
   * to this repository, so the query uses {@code ILIKE} with the escape character '\'.
   */
  /**
   * <p>Because PostgreSQL cannot infer the data type of a named parameter that is used
   * multiple times in a native query, we rewrite the query to use positional
   * parameters.  JPA will bind the same value for every occurrence of the same
   * index, preserving the original semantics.</p>
   */
  @Query(
      value = """
      SELECT DISTINCT pm.player_id
      FROM player_matches pm
      JOIN players p ON p.id = pm.player_id
      JOIN matches m ON m.id = pm.match_id
      WHERE (CAST(:champion AS text) IS NULL OR
            replace(lower(pm.champion_name), 'ς', 'σ') ILIKE CONCAT('%', replace(lower(:champion),'ς','σ'),'%') )
        AND (CAST(:text AS text) IS NULL OR trim(lower(:text)) = '' OR (
          replace(lower(pm.champion_name), 'ς', 'σ') ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%')
          OR replace(lower(TRIM(m.game_mode)), 'ς', 'σ') ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%')
          OR replace(lower(m.match_id), 'ς', 'σ') ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%')
          OR replace(lower(pm.result::text), 'ς', 'σ') ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%')
          OR replace(lower(p.game_name), 'ς', 'σ') ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%')
          OR replace(lower(p.tag_line), 'ς', 'σ') ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%')
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
              ) ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%')
          )
          -- Known queue labels
          OR (m.queue_id = 0 AND 'Custom' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 400 AND 'Normal Draft' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 420 AND 'Ranked Solo/Duo' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 430 AND 'Normal Blind' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 440 AND 'Ranked Flex' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 450 AND 'ARAM' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 490 AND 'Quickplay' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 700 AND 'Clash' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 830 AND 'Co-op vs AI Intro' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 840 AND 'Co-op vs AI Beginner' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 850 AND 'Co-op vs AI Intermediate' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 1700 AND 'Arena' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
          OR (m.queue_id = 1710 AND 'Arena' ILIKE CONCAT('%', replace(lower(:text),'ς','σ'),'%') )
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
