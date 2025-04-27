package com.loltracker.matchhistoryservice.infrastructure.repositories;

import com.loltracker.matchhistoryservice.infrastructure.models.PlayerMatchMO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerMatchRepository extends JpaRepository<PlayerMatchMO, String> {}
