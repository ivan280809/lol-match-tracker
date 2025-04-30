package com.loltracker.matchhistoryservice.domain;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.domain.services.MatchHistoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MatchHistoryUseCase {

  private final MatchHistoryService matchHistoryService;

  public void processMatchHistory(AccountMatchesDTO matches) {
    matchHistoryService.processMatchHistory(matches);
  }
}
