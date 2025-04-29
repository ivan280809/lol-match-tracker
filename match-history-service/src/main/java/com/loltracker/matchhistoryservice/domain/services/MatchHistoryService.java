package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MatchHistoryService {
//REHACER REVISAR SI HACEN FALTA TODOS LOS REPOS Y CREAR LOS MAPPERS :)
  private final LolPlayerHeaderRepository lolPlayerHeaderRepository;
  private final PlayerMatchRepository playerMatchRepository;

  public String processMatchHistory(AccountMatchesDTO matches) {
    ensurePlayerHeaderExists(matches.getAccountDTO());

    List<String> matchesSaved = new ArrayList<>();

    matches
        .getMatches()
        .forEach(
            match -> {
              long matchId = generateMatchId(puuid, match);

              boolean alreadyExists = playerMatchRepository.findById(matchId).isPresent();

              if (!alreadyExists) {
                playerMatchRepository.save(new PlayerMatchMO(matchId, puuid, match));
                matchesSaved.add(matchId + "");
              }
            });

    return "Processed match history for PUUID: "
        + puuid
        + ", new matches saved: "
        + matchesSaved.size();
  }

  private void ensurePlayerHeaderExists(String puuid) {
    boolean exists = lolPlayerHeaderRepository.findById(puuid).isPresent();
    if (!exists) {
      lolPlayerHeaderRepository.save(LolPlayerHeaderMO.builder().puuid(puuid).build());
    }
  }

  private long generateMatchId(String puuid, Object match) {
    return (puuid + "-" + match.hashCode()).hashCode();
  }
}
