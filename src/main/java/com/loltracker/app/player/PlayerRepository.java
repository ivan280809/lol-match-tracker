package com.loltracker.app.player;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

  List<PlayerEntity> findAllByActiveTrueOrderByGameNameAsc();

  Optional<PlayerEntity> findByGameNameIgnoreCaseAndTagLineIgnoreCase(String gameName, String tagLine);
}

