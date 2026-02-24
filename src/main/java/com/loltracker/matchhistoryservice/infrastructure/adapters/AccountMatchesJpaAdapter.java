package com.loltracker.matchhistoryservice.infrastructure.adapters;

import com.loltracker.matchhistoryservice.domain.ports.out.AccountMatchesStorePort;
import com.loltracker.matchhistoryservice.infrastructure.models.AccountMatchesMO;
import com.loltracker.matchhistoryservice.infrastructure.repositories.AccountMatchesRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountMatchesJpaAdapter implements AccountMatchesStorePort {

  private final AccountMatchesRepository accountMatchesRepository;

  @Override
  public Optional<AccountMatchesMO> findByPuuid(String puuid) {
    return accountMatchesRepository.findById(puuid);
  }

  @Override
  public AccountMatchesMO save(AccountMatchesMO accountMatchesMO) {
    return accountMatchesRepository.save(accountMatchesMO);
  }
}
