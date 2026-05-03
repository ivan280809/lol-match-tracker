package com.loltracker.app.notification;

import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.player.PlayerRankService;
import com.loltracker.app.player.PlayerRankSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationStatsService {

  private static final ZoneId MADRID_ZONE = ZoneId.of("Europe/Madrid");

  private final TrackedMatchRepository trackedMatchRepository;
  private final PlayerRankService playerRankService;

  @Transactional
  public NotificationStatsSnapshot buildFor(TrackedMatchEntity match) {
    Long playerId = match.getPlayer().getId();
    Optional<PlayerRankSnapshot> playerRank =
        playerRankService.refreshRank(match.getPlayer(), match.getPlayer().getPuuid());
    Optional<PlayerRankSnapshot> rosterAverageRank = playerRankService.rosterAverageRank();
    List<TrackedMatchEntity> recentMatches =
        trackedMatchRepository.findTop10ByPlayerIdOrderByGameEndAtDesc(playerId);
    List<TrackedMatchEntity> championMatches =
        trackedMatchRepository.findTop20ByPlayerIdAndChampionNameIgnoreCaseOrderByGameEndAtDesc(
            playerId, match.getChampionName());
    List<TrackedMatchEntity> dayMatches = findMatchesFromSameLocalDay(match, playerId);

    return new NotificationStatsSnapshot(
        recentForm(recentMatches),
        currentStreakCount(recentMatches),
        currentStreakResult(recentMatches),
        wins(dayMatches),
        losses(dayMatches),
        wins(championMatches),
        losses(championMatches),
        averageDuration(recentMatches),
        playerRank.map(PlayerRankSnapshot::displayName).orElse("Sin rank disponible"),
        rosterAverageRank.map(PlayerRankSnapshot::displayName).orElse("Sin media disponible"),
        rankDelta(playerRank, rosterAverageRank));
  }

  private Integer rankDelta(
      Optional<PlayerRankSnapshot> playerRank, Optional<PlayerRankSnapshot> rosterAverageRank) {
    if (playerRank.isEmpty() || rosterAverageRank.isEmpty()) {
      return null;
    }
    return playerRank.get().score() - rosterAverageRank.get().score();
  }

  private List<TrackedMatchEntity> findMatchesFromSameLocalDay(
      TrackedMatchEntity match, Long playerId) {
    LocalDate matchDate = LocalDate.ofInstant(match.getGameEndAt(), MADRID_ZONE);
    Instant startInclusive = matchDate.atStartOfDay(MADRID_ZONE).toInstant();
    Instant endExclusive = matchDate.plusDays(1).atStartOfDay(MADRID_ZONE).toInstant();
    return trackedMatchRepository
        .findAllByPlayerIdAndGameEndAtGreaterThanEqualAndGameEndAtLessThanOrderByGameEndAtDesc(
            playerId, startInclusive, endExclusive);
  }

  private String recentForm(List<TrackedMatchEntity> matches) {
    if (matches.isEmpty()) {
      return "Sin historial";
    }
    return matches.stream().map(this::shortResult).reduce((left, right) -> left + " " + right).orElse("");
  }

  private int currentStreakCount(List<TrackedMatchEntity> matches) {
    if (matches.isEmpty()) {
      return 0;
    }
    String firstResult = matches.get(0).getResult();
    int streak = 0;
    for (TrackedMatchEntity match : matches) {
      if (!match.getResult().equalsIgnoreCase(firstResult)) {
        break;
      }
      streak++;
    }
    return streak;
  }

  private String currentStreakResult(List<TrackedMatchEntity> matches) {
    if (matches.isEmpty()) {
      return "sin datos";
    }
    return isVictory(matches.get(0)) ? "victoria" : "derrota";
  }

  private String shortResult(TrackedMatchEntity match) {
    return isVictory(match) ? "W" : "L";
  }

  private int wins(List<TrackedMatchEntity> matches) {
    return (int) matches.stream().filter(this::isVictory).count();
  }

  private int losses(List<TrackedMatchEntity> matches) {
    return matches.size() - wins(matches);
  }

  private long averageDuration(List<TrackedMatchEntity> matches) {
    return Math.round(matches.stream().mapToLong(TrackedMatchEntity::getDurationSeconds).average().orElse(0));
  }

  private boolean isVictory(TrackedMatchEntity match) {
    return "VICTORY".equalsIgnoreCase(match.getResult());
  }
}
