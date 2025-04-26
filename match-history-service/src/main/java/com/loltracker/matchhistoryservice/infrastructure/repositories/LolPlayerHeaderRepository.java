package com.loltracker.matchhistoryservice.infrastructure.repositories;

import com.loltracker.matchhistoryservice.infrastructure.models.LolPlayerHeaderMO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolPlayerHeaderRepository extends JpaRepository<LolPlayerHeaderMO, String> {}
