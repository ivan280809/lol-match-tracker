package com.loltracker.app.integration.riot;

import com.loltracker.app.match.MatchSummary;
import java.time.Instant;
import java.util.List;

public record RiotMatchDetails(
    String matchId,
    String gameMode,
    Integer queueId,
    long durationSeconds,
    Instant gameEndAt,
    String platform,
    String region,
    List<RiotMatchParticipant> participants) {

  public MatchSummary summaryFor(String puuid) {
    RiotMatchParticipant participant =
        participants.stream()
            .filter(candidate -> puuid.equals(candidate.puuid()))
            .findFirst()
            .orElseThrow(() -> new RiotApiException(
                RiotErrorCategory.MALFORMED_RESPONSE,
                "Riot respondio sin la participacion esperada para el jugador"));
    return new MatchSummary(
        matchId,
        participant.championId(),
        participant.championName(),
        participant.win(),
        gameMode,
        queueId,
        participant.lane(),
        participant.role(),
        participant.kills(),
        participant.deaths(),
        participant.assists(),
        participant.creepScore(),
        participant.goldEarned(),
        participant.damageDealtToChampions(),
        participant.visionScore(),
        durationSeconds,
        gameEndAt,
        platform,
        region);
  }
}
