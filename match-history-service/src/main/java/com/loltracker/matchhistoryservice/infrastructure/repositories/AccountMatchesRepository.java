package com.loltracker.matchhistoryservice.infrastructure.repositories;

import com.loltracker.matchhistoryservice.infrastructure.models.AccountMatchesMO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountMatchesRepository extends JpaRepository<AccountMatchesMO, Long> {}
