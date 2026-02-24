package com.loltracker.playerservices.domain.ports.out;

import com.loltracker.playerservices.infrastructure.models.AccountMatchesDTO;
import reactor.core.publisher.Mono;

public interface MatchHistoryPort {
  Mono<String> putMatches(AccountMatchesDTO accountMatchesDTO);

  Mono<Boolean> matchExists(String puuid, String matchId);
}
