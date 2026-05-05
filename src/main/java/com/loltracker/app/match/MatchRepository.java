package com.loltracker.app.match;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

  Optional<MatchEntity> findByMatchId(String matchId);
}
