package com.loltracker.matchhistoryservice.domain.ports.out;

import com.loltracker.matchhistoryservice.infrastructure.models.AccountMatchesMO;
import java.util.Optional;

public interface AccountMatchesStorePort {
  Optional<AccountMatchesMO> findByPuuid(String puuid);

  AccountMatchesMO save(AccountMatchesMO accountMatchesMO);
}
