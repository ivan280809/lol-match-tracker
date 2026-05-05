package com.loltracker.app.match;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatchEntity, Long> {

  boolean existsByPlayerIdAndMatchMatchId(Long playerId, String matchId);

  Optional<PlayerMatchEntity> findByPlayerIdAndMatchMatchId(Long playerId, String matchId);
}
