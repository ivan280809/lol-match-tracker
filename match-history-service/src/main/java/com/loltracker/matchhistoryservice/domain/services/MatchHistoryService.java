package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.controllers.model.MatchesDTO;
import com.loltracker.matchhistoryservice.infrastructure.models.LolPlayerHeaderMO;
import com.loltracker.matchhistoryservice.infrastructure.models.PlayerMatchMO;
import com.loltracker.matchhistoryservice.infrastructure.repositories.LolPlayerHeaderRepository;
import com.loltracker.matchhistoryservice.infrastructure.repositories.PlayerMatchRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MatchHistoryService {

  private final LolPlayerHeaderRepository lolPlayerHeaderRepository;
  private final PlayerMatchRepository playerMatchRepository;

  public String processMatchHistory(String puuid, MatchesDTO matches) {
    matches
        .getMatches()
        .forEach(
            match -> {
              playerMatchRepository.save(new PlayerMatchMO(puuid, match));
            });

    lolPlayerHeaderRepository.save(LolPlayerHeaderMO.builder().puuid(puuid).build());

    return "Match history saved successfully for PUUID: " + puuid;
  }
}
