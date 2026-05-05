package com.loltracker.app.integration.riot;

import com.loltracker.app.match.MatchSummary;
import java.util.List;

public interface RiotMatchPort {

  default List<String> fetchRecentMatchIds(String puuid) {
    return fetchRecentMatchIds(puuid, 0, 10);
  }

  List<String> fetchRecentMatchIds(String puuid, int start, int count);

  RiotMatchDetails fetchMatchDetails(String matchId);

  default MatchSummary fetchMatchSummary(String matchId, String puuid) {
    return fetchMatchDetails(matchId).summaryFor(puuid);
  }
}
