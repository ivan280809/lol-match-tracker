package com.loltracker.app.notification;

import com.loltracker.app.match.MatchQueueCatalog;
import com.loltracker.app.match.MatchQueueDescriptor;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.player.PlayerRankService;
import com.loltracker.app.player.PlayerRankService.RankAvailability;
import com.loltracker.app.player.PlayerRankService.RankRefreshResult;
import com.loltracker.app.player.PlayerRankSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NotificationStatsService {

  private final TrackedMatchRepository trackedMatchRepository;
  private final PlayerRankService playerRankService;
  private final ZoneId appZoneId;

  public NotificationStatsService(
      TrackedMatchRepository trackedMatchRepository, PlayerRankService playerRankService) {
    this(trackedMatchRepository, playerRankService, ZoneId.of("Europe/Madrid"));
  }

  @Autowired
  public NotificationStatsService(
      TrackedMatchRepository trackedMatchRepository,
      PlayerRankService playerRankService,
      ZoneId appZoneId) {
    this.trackedMatchRepository = trackedMatchRepository;
    this.playerRankService = playerRankService;
    this.appZoneId = appZoneId;
  }

  public NotificationStatsSnapshot buildFor(TrackedMatchEntity match) {
    Long playerId = match.getPlayer().getId();
    RankQueueContext rankQueue = rankQueueContext(match);
    RankRefreshResult playerRank =
        playerRankService.refreshRankAvailabilityForQueue(
            match.getPlayer(), match.getPlayer().getPuuid(), rankQueue.queueType());
    Optional<PlayerRankSnapshot> rosterAverageRank = playerRankService.rosterAverageRank();
    List<TrackedMatchEntity> recentMatches = recentMatches(playerId);
    List<TrackedMatchEntity> recentFormMatches =
        safeList(trackedMatchRepository.findTop10ByPlayerIdOrderByGameEndAtDesc(playerId));
    if (recentFormMatches.isEmpty()) {
      recentFormMatches = recentMatches.stream().limit(10).toList();
    }
    List<TrackedMatchEntity> championMatches =
        safeList(
            trackedMatchRepository
                .findTop20ByPlayerIdAndChampionNameIgnoreCaseOrderByGameEndAtDesc(
                    playerId, match.getChampionName()));
    List<TrackedMatchEntity> queueMatches = queueMatches(playerId, match);
    List<TrackedMatchEntity> positionMatches = positionMatches(playerId, match);
    List<TrackedMatchEntity> dayMatches = findMatchesFromSameLocalDay(match, playerId);
    NotificationPerformanceProfile recentProfile = profile("Ult. 30", recentMatches);
    NotificationPerformanceProfile queueProfile = profile(rankQueue.label(), queueMatches);
    NotificationPerformanceProfile championProfile = profile(match.getChampionName(), championMatches);
    NotificationPerformanceProfile positionProfile = profile(positionLabel(match), positionMatches);
    NotificationPerformanceDelta delta = delta(match, baseline(recentMatches, match));
    List<NotificationSharedPlayer> sharedPlayers = sharedPlayersFor(match);

    return new NotificationStatsSnapshot(
        recentForm(recentFormMatches),
        currentStreakCount(recentFormMatches),
        currentStreakResult(recentFormMatches),
        wins(dayMatches),
        losses(dayMatches),
        wins(championMatches),
        losses(championMatches),
        averageDuration(recentMatches.isEmpty() ? recentFormMatches : recentMatches),
        formatPlayerRankValue(playerRank),
        formatPlayerRankNote(playerRank, rankQueue),
        formatPlayerRankQueueLabel(playerRank, rankQueue),
        rosterAverageRank.map(PlayerRankSnapshot::displayName).orElse("Sin media disponible"),
        rankDelta(playerRank.snapshot(), rosterAverageRank),
        sharedPlayers,
        recentProfile,
        queueProfile,
        championProfile,
        positionProfile,
        delta,
        highlights(match, recentFormMatches, championProfile, delta, sharedPlayers));
  }

  private String formatPlayerRankValue(RankRefreshResult playerRank) {
    return switch (playerRank.availability()) {
      case CURRENT, STORED, REFRESH_ERROR_STORED -> displaySnapshot(playerRank);
      case UNRANKED -> "Unranked";
      case NO_RANK -> "Sin rank registrado";
      case REFRESH_ERROR_NO_STORED -> "Sin rank actualizado";
    };
  }

  private String formatPlayerRankNote(RankRefreshResult playerRank, RankQueueContext rankQueue) {
    return switch (playerRank.availability()) {
      case CURRENT -> "actualizado ahora";
      case STORED -> "guardado";
      case UNRANKED -> rankQueue.specific() ? "sin rank " + rankQueue.label() : "sin SoloQ/Flex";
      case NO_RANK -> "";
      case REFRESH_ERROR_STORED -> "guardado; no se pudo actualizar";
      case REFRESH_ERROR_NO_STORED -> "no se pudo consultar Riot";
    };
  }

  private String formatPlayerRankQueueLabel(RankRefreshResult playerRank, RankQueueContext rankQueue) {
    if (rankQueue.specific()) {
      return rankQueue.label();
    }
    return playerRank
        .snapshot()
        .map(PlayerRankSnapshot::displayQueueLabel)
        .orElse(rankQueue.label());
  }

  private RankQueueContext rankQueueContext(TrackedMatchEntity match) {
    MatchQueueDescriptor queue = MatchQueueCatalog.describe(match.getQueueId(), match.getGameMode());
    return switch (queue.type()) {
      case RANKED_SOLO -> new RankQueueContext("Solo/Duo", PlayerRankService.SOLO_QUEUE, true);
      case RANKED_FLEX -> new RankQueueContext("Flex", PlayerRankService.FLEX_QUEUE, true);
      default -> new RankQueueContext("Mejor rank", null, false);
    };
  }

  private String displaySnapshot(RankRefreshResult playerRank) {
    return playerRank
        .snapshot()
        .map(PlayerRankSnapshot::displayName)
        .orElseGet(() -> fallbackRankLabel(playerRank.availability()));
  }

  private String fallbackRankLabel(RankAvailability availability) {
    return switch (availability) {
      case UNRANKED -> "Unranked";
      case NO_RANK -> "Sin rank registrado";
      case REFRESH_ERROR_NO_STORED -> "Sin rank actualizado";
      default -> "Sin rank disponible";
    };
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
    LocalDate matchDate = LocalDate.ofInstant(match.getGameEndAt(), appZoneId);
    Instant startInclusive = matchDate.atStartOfDay(appZoneId).toInstant();
    Instant endExclusive = matchDate.plusDays(1).atStartOfDay(appZoneId).toInstant();
    return safeList(
        trackedMatchRepository
            .findAllByPlayerIdAndGameEndAtGreaterThanEqualAndGameEndAtLessThanOrderByGameEndAtDesc(
                playerId, startInclusive, endExclusive));
  }

  private List<TrackedMatchEntity> recentMatches(Long playerId) {
    return safeList(
        trackedMatchRepository.findByPlayerIdOrderByGameEndAtDesc(playerId, PageRequest.of(0, 30)));
  }

  private List<TrackedMatchEntity> queueMatches(Long playerId, TrackedMatchEntity match) {
    if (match.getQueueId() == null) {
      return List.of();
    }
    return safeList(
        trackedMatchRepository.findTop20ByPlayerIdAndQueueIdOrderByGameEndAtDesc(
            playerId, match.getQueueId()));
  }

  private List<TrackedMatchEntity> positionMatches(Long playerId, TrackedMatchEntity match) {
    if (match.getLane() != null && !match.getLane().isBlank()) {
      return safeList(
          trackedMatchRepository.findTop20ByPlayerIdAndLaneIgnoreCaseOrderByGameEndAtDesc(
              playerId, match.getLane()));
    }
    if (match.getRole() != null && !match.getRole().isBlank()) {
      return safeList(
          trackedMatchRepository.findTop20ByPlayerIdAndRoleIgnoreCaseOrderByGameEndAtDesc(
              playerId, match.getRole()));
    }
    return List.of();
  }

  private List<NotificationSharedPlayer> sharedPlayersFor(TrackedMatchEntity match) {
    if (match.getMatchId() == null || match.getMatchId().isBlank()) {
      return List.of();
    }
    List<TrackedMatchEntity> sharedMatches =
        safeList(trackedMatchRepository.findAllByMatchIdOrderByIdAsc(match.getMatchId()));
    if (sharedMatches.size() < 2) {
      return List.of();
    }
    return sharedMatches.stream()
        .map(
            shared ->
                new NotificationSharedPlayer(
                    shared.getPlayer().getGameName() + "#" + shared.getPlayer().getTagLine(),
                    shared.getChampionName(),
                    shortResult(shared),
                    shared.getKills(),
                    shared.getDeaths(),
                    shared.getAssists()))
        .toList();
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

  private NotificationPerformanceProfile profile(String label, List<TrackedMatchEntity> matches) {
    if (matches == null || matches.isEmpty()) {
      return NotificationPerformanceProfile.empty(label);
    }
    int wins = wins(matches);
    int total = matches.size();
    return new NotificationPerformanceProfile(
        label,
        total,
        wins,
        total - wins,
        percentage(wins, total),
        roundOneDecimal(matches.stream().mapToInt(TrackedMatchEntity::getKills).average().orElse(0)),
        roundOneDecimal(matches.stream().mapToInt(TrackedMatchEntity::getDeaths).average().orElse(0)),
        roundOneDecimal(matches.stream().mapToInt(TrackedMatchEntity::getAssists).average().orElse(0)),
        roundTwoDecimals(matches.stream().mapToDouble(this::kdaRatio).average().orElse(0)),
        roundOneDecimal(matches.stream().mapToDouble(match -> perMinute(match.getCreepScore(), match)).average().orElse(0)),
        roundOneDecimal(matches.stream().mapToDouble(match -> perMinute(match.getGoldEarned(), match)).average().orElse(0)),
        roundOneDecimal(
            matches.stream()
                .mapToDouble(match -> perMinute(match.getDamageDealtToChampions(), match))
                .average()
                .orElse(0)),
        roundOneDecimal(matches.stream().mapToDouble(match -> perMinute(match.getVisionScore(), match)).average().orElse(0)));
  }

  private NotificationPerformanceDelta delta(
      TrackedMatchEntity match, List<TrackedMatchEntity> baselineMatches) {
    if (baselineMatches.isEmpty()) {
      return NotificationPerformanceDelta.empty();
    }
    NotificationPerformanceProfile baseline = profile("base", baselineMatches);
    return new NotificationPerformanceDelta(
        true,
        roundTwoDecimals(kdaRatio(match) - baseline.averageKdaRatio()),
        roundOneDecimal(perMinute(match.getCreepScore(), match) - baseline.averageCsPerMinute()),
        roundOneDecimal(perMinute(match.getGoldEarned(), match) - baseline.averageGoldPerMinute()),
        roundOneDecimal(
            perMinute(match.getDamageDealtToChampions(), match)
                - baseline.averageDamagePerMinute()),
        roundOneDecimal(perMinute(match.getVisionScore(), match) - baseline.averageVisionPerMinute()));
  }

  private List<TrackedMatchEntity> baseline(
      List<TrackedMatchEntity> recentMatches, TrackedMatchEntity current) {
    List<TrackedMatchEntity> baseline =
        recentMatches.stream().filter(match -> !sameStoredMatch(match, current)).toList();
    return baseline.isEmpty() ? recentMatches : baseline;
  }

  private List<String> highlights(
      TrackedMatchEntity match,
      List<TrackedMatchEntity> recentMatches,
      NotificationPerformanceProfile championProfile,
      NotificationPerformanceDelta delta,
      List<NotificationSharedPlayer> sharedPlayers) {
    List<String> highlights = new ArrayList<>();
    if (match.getDeaths() == 0 && match.getKills() + match.getAssists() > 0) {
      highlights.add("KDA perfecto");
    }
    int streak = currentStreakCount(recentMatches);
    if (streak >= 3) {
      highlights.add(streak + " " + currentStreakResult(recentMatches) + "s seguidas");
    }
    if (championProfile.games() >= 3 && championProfile.winRate() >= 60) {
      highlights.add(championProfile.label() + " fuerte: " + championProfile.winRate() + "% WR");
    } else if (championProfile.games() >= 3 && championProfile.winRate() <= 40) {
      highlights.add(championProfile.label() + " en alerta: " + championProfile.winRate() + "% WR");
    }
    if (delta.available()) {
      if (delta.damagePerMinuteDelta() >= 100) {
        highlights.add("Dano/min muy por encima de media");
      }
      if (delta.csPerMinuteDelta() >= 1) {
        highlights.add("CS/min por encima de media");
      }
      if (delta.visionPerMinuteDelta() >= 0.4) {
        highlights.add("Vision/min por encima de media");
      }
    }
    if (sharedPlayers.size() > 1) {
      highlights.add("Partida compartida con " + (sharedPlayers.size() - 1) + " tracked");
    }
    return highlights.stream().limit(4).toList();
  }

  private boolean sameStoredMatch(TrackedMatchEntity left, TrackedMatchEntity right) {
    if (left.getId() != null && right.getId() != null) {
      return left.getId().equals(right.getId());
    }
    return left.getMatchId() != null && left.getMatchId().equals(right.getMatchId());
  }

  private double kdaRatio(TrackedMatchEntity match) {
    if (match.getDeaths() == 0) {
      return match.getKills() + match.getAssists();
    }
    return (match.getKills() + match.getAssists()) / (double) match.getDeaths();
  }

  private double perMinute(long value, TrackedMatchEntity match) {
    if (match.getDurationSeconds() <= 0) {
      return 0;
    }
    return value / (match.getDurationSeconds() / 60.0);
  }

  private int percentage(int value, int total) {
    if (total <= 0) {
      return 0;
    }
    return Math.round((value * 100.0f) / total);
  }

  private double roundOneDecimal(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private double roundTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  private String positionLabel(TrackedMatchEntity match) {
    String lane = match.getLane() == null ? "" : match.getLane().trim();
    String role = match.getRole() == null ? "" : match.getRole().trim();
    if (lane.isBlank() && role.isBlank()) {
      return "Posicion";
    }
    if (lane.isBlank()) {
      return role;
    }
    if (role.isBlank() || role.equalsIgnoreCase(lane)) {
      return lane;
    }
    return lane + "/" + role;
  }

  private List<TrackedMatchEntity> safeList(List<TrackedMatchEntity> matches) {
    return matches == null ? List.of() : matches;
  }

  private boolean isVictory(TrackedMatchEntity match) {
    return "VICTORY".equalsIgnoreCase(match.getResult());
  }

  private record RankQueueContext(String label, String queueType, boolean specific) {}
}
