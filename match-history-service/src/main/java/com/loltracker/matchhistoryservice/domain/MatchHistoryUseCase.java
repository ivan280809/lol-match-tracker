package com.loltracker.matchhistoryservice.domain;

import com.loltracker.matchhistoryservice.controllers.model.MatchesDTO;
import com.loltracker.matchhistoryservice.domain.services.MatchHistoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MatchHistoryUseCase {

  private final MatchHistoryService matchHistoryService;

  public String processMatchHistory(String puuid, MatchesDTO matches) {
    return matchHistoryService.processMatchHistory(puuid, matches);
  }
}
