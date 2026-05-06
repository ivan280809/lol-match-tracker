package com.loltracker.app.player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRankRepository extends JpaRepository<PlayerRankEntity, Long> {

  Optional<PlayerRankEntity> findByPlayerIdAndQueueType(Long playerId, String queueType);

  List<PlayerRankEntity> findAllByPlayerIdAndQueueTypeIn(Long playerId, Collection<String> queueTypes);
}
