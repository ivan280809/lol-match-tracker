package com.loltracker.app.player;

import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.riot.RiotRankEntry;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerRankService {

  private static final String SOLO_QUEUE = "RANKED_SOLO_5x5";
  private static final String FLEX_QUEUE = "RANKED_FLEX_SR";

  private final RiotClient riotClient;
  private final PlayerRepository playerRepository;

  @Transactional
  public Optional<PlayerRankSnapshot> refreshRank(PlayerEntity player, String puuid) {
    if (puuid == null || puuid.isBlank()) {
      return storedRank(player);
    }
    try {
      Optional<PlayerRankSnapshot> snapshot =
          selectBestRank(riotClient.fetchRankEntries(RiotPlatform.fromFormValue(player.getPlatform()), puuid));
      applySnapshot(player, snapshot.orElse(null));
      playerRepository.save(player);
      return snapshot;
    } catch (RuntimeException e) {
      log.warn("Rank refresh failed for {}#{}", player.getGameName(), player.getTagLine(), e);
      return storedRank(player);
    }
  }

  @Transactional(readOnly = true)
  public Optional<PlayerRankSnapshot> rosterAverageRank() {
    List<Integer> scores =
        playerRepository.findAllByActiveTrueOrderByGameNameAsc().stream()
            .map(PlayerEntity::getRankScore)
            .filter(score -> score != null && score >= 0)
            .toList();
    if (scores.isEmpty()) {
      return Optional.empty();
    }
    int average = Math.round((float) scores.stream().mapToInt(Integer::intValue).average().orElse(0));
    return Optional.of(fromScore(average));
  }

  public Optional<PlayerRankSnapshot> storedRank(PlayerEntity player) {
    if (player.getRankScore() == null
        || player.getRankTier() == null
        || player.getRankDivision() == null
        || player.getRankLeaguePoints() == null) {
      return Optional.empty();
    }
    return Optional.of(
        new PlayerRankSnapshot(
            player.getRankQueueType(),
            player.getRankTier(),
            player.getRankDivision(),
            player.getRankLeaguePoints(),
            player.getRankScore()));
  }

  private Optional<PlayerRankSnapshot> selectBestRank(List<RiotRankEntry> entries) {
    return entries.stream()
        .filter(entry -> SOLO_QUEUE.equals(entry.queueType()) || FLEX_QUEUE.equals(entry.queueType()))
        .min(Comparator.comparingInt(this::queuePriority))
        .map(
            entry ->
                new PlayerRankSnapshot(
                    entry.queueType(),
                    entry.tier(),
                    normalizeDivision(entry.rank()),
                    entry.leaguePoints(),
                    score(entry.tier(), entry.rank(), entry.leaguePoints())));
  }

  private int queuePriority(RiotRankEntry entry) {
    return SOLO_QUEUE.equals(entry.queueType()) ? 0 : 1;
  }

  private void applySnapshot(PlayerEntity player, PlayerRankSnapshot snapshot) {
    if (snapshot == null) {
      player.setRankQueueType(null);
      player.setRankTier(null);
      player.setRankDivision(null);
      player.setRankLeaguePoints(null);
      player.setRankScore(null);
      player.setRankUpdatedAt(Instant.now());
      return;
    }
    player.setRankQueueType(snapshot.queueType());
    player.setRankTier(snapshot.tier());
    player.setRankDivision(snapshot.division());
    player.setRankLeaguePoints(snapshot.leaguePoints());
    player.setRankScore(snapshot.score());
    player.setRankUpdatedAt(Instant.now());
  }

  private PlayerRankSnapshot fromScore(int score) {
    String tier = tierFromScore(score);
    int tierBase = tierBase(tier);
    int tierProgress = Math.max(0, score - tierBase);
    int divisionStep = Math.min(3, tierProgress / 100);
    int leaguePoints = Math.max(0, Math.min(99, tierProgress % 100));
    return new PlayerRankSnapshot("ROSTER_AVERAGE", tier, divisionFromStep(divisionStep), leaguePoints, score);
  }

  private int score(String tier, String division, int leaguePoints) {
    return tierBase(tier) + divisionBase(division) + Math.max(0, leaguePoints);
  }

  private String tierFromScore(int score) {
    if (score >= 3600) return "CHALLENGER";
    if (score >= 3200) return "GRANDMASTER";
    if (score >= 2800) return "MASTER";
    if (score >= 2400) return "DIAMOND";
    if (score >= 2000) return "EMERALD";
    if (score >= 1600) return "PLATINUM";
    if (score >= 1200) return "GOLD";
    if (score >= 800) return "SILVER";
    if (score >= 400) return "BRONZE";
    return "IRON";
  }

  private int tierBase(String tier) {
    return switch (safeUpper(tier)) {
      case "BRONZE" -> 400;
      case "SILVER" -> 800;
      case "GOLD" -> 1200;
      case "PLATINUM" -> 1600;
      case "EMERALD" -> 2000;
      case "DIAMOND" -> 2400;
      case "MASTER" -> 2800;
      case "GRANDMASTER" -> 3200;
      case "CHALLENGER" -> 3600;
      default -> 0;
    };
  }

  private int divisionBase(String division) {
    return switch (normalizeDivision(division)) {
      case "III" -> 100;
      case "II" -> 200;
      case "I" -> 300;
      default -> 0;
    };
  }

  private String divisionFromStep(int step) {
    return switch (step) {
      case 3 -> "I";
      case 2 -> "II";
      case 1 -> "III";
      default -> "IV";
    };
  }

  private String normalizeDivision(String division) {
    String normalized = safeUpper(division);
    return normalized.isBlank() ? "IV" : normalized;
  }

  private String safeUpper(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }
}
