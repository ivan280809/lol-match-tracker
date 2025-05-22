package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.infrastructure.models.MatchMO;
import com.loltracker.matchhistoryservice.infrastructure.models.ParticipantMO;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MatchMessageBuilder {
  public String buildMatchMessage(MatchMO match, String gameName) {
    Optional<ParticipantMO> participant = getParticipantByGameName(match, gameName);
    return "Game Name: "
        + gameName
        + "\n"
        + "Champion: "
        + participant.map(ParticipantMO::getChampionName).orElse("Unknown")
        + "\n"
        + "End of Game Result: "
        + getEndOfGameResult(match, participant)
        + "\n"
        + "Game Mode: "
        + match.getInfo().getGameMode()
        + "\n"
        + "Game Duration (seconds): "
        + match.getInfo().getGameDuration()
        + "\n"
        + "Game End Timestamp: "
        + getEndTime(match);
  }

  private Optional<ParticipantMO> getParticipantByGameName(MatchMO match, String gameName) {
    return match.getInfo().getParticipants().stream()
        .filter(player -> player.getRiotIdGameName().equals(gameName))
        .findFirst();
  }

  private String getEndOfGameResult(MatchMO match, Optional<ParticipantMO> participantMO) {
    return participantMO
        .map(ParticipantMO::isWin)
        .map(win -> win ? "Victory" : "Defeat")
        .orElse("Unknown");
  }

  private String getEndTime(MatchMO match) {
    long timestampMillis = match.getInfo().getGameEndTimestamp();

    ZonedDateTime dateTimeInSpain = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.of("Europe/Madrid"));

    return dateTimeInSpain.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
  }
}
