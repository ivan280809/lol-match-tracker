package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.infrastructure.models.MatchMO;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MatchAnalyzerService {

  private final MatchNotificationService matchNotificationService;

  public void analyzeMatches(String gameName, List<MatchMO> newMatches) {
    newMatches.forEach(
        match -> {
          String endOfGameResult = match.getInfo().getEndOfGameResult();
          String gameMode = match.getInfo().getGameMode();
          matchNotificationService.notifyMatchHistory(gameName, endOfGameResult, gameMode);
        });
  }
}
