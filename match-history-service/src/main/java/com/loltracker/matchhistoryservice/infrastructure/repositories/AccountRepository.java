package com.loltracker.matchhistoryservice.infrastructure.repositories;

import com.loltracker.matchhistoryservice.infrastructure.models.AccountMO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountMO, String> {}
