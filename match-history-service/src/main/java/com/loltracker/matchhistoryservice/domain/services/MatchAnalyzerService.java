package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.infrastructure.models.MatchMO;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MatchAnalyzerService {

  private final MatchNotificationService matchNotificationService;
  private final MatchMessageBuilder matchMessageBuilder;

  public void analyzeMatches(String gameName, List<MatchMO> newMatches) {
    newMatches.forEach(match -> processNewMatches(match, gameName));
  }

  private void processNewMatches(MatchMO match, String gameName) {
    String message = matchMessageBuilder.buildMatchMessage(match, gameName);
    matchNotificationService.notifyMatchHistory(message);
  }
}
