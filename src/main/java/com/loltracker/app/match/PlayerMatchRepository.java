package com.loltracker.app.match;

import java.util.Optional;
import java.util.List;
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
      """)
  List<PlayerMatchEntity> findRecent(Pageable pageable);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      where pm.player.id = :playerId
      order by m.gameEndAt desc, pm.id desc
      """)
  List<PlayerMatchEntity> findRecentForPlayer(@Param("playerId") Long playerId, Pageable pageable);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      """)
  List<PlayerMatchEntity> findAllForHistorySearch();

  @Query(
      """
      select m.id
      from PlayerMatchEntity pm
      join pm.match m
      group by m.id
      having count(pm.id) > 1
      order by max(m.gameEndAt) desc
      """)
  List<Long> findSharedMatchIds(Pageable pageable);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      where m.id in :matchIds
      order by m.gameEndAt desc, pm.id asc
      """)
  List<PlayerMatchEntity> findByMatchIdsWithPlayerAndMatch(@Param("matchIds") List<Long> matchIds);

  @Query(
      """
      select pm
      from PlayerMatchEntity pm
      join fetch pm.player
      join fetch pm.match m
      where m.matchId = :matchId
      order by pm.id asc
      """)
  List<PlayerMatchEntity> findByMatchMatchIdWithPlayerAndMatch(@Param("matchId") String matchId);
}
