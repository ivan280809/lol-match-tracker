package com.loltracker.app.player;

import com.loltracker.app.integration.riot.RiotRankEntry;
import com.loltracker.app.integration.riot.RiotRankPort;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class PlayerRankService {

  public static final String SOLO_QUEUE = "RANKED_SOLO_5x5";
  public static final String FLEX_QUEUE = "RANKED_FLEX_SR";
  private static final Set<String> SUPPORTED_RANK_QUEUES = Set.of(SOLO_QUEUE, FLEX_QUEUE);

  private final RiotRankPort riotRankPort;
  private final PlayerRepository playerRepository;
  private final PlayerRankRepository playerRankRepository;
  private final Clock clock;

  public PlayerRankService(
      com.loltracker.app.integration.riot.RiotClient riotClient, PlayerRepository playerRepository) {
    this(riotClient, playerRepository, null, Clock.systemUTC());
  }

  @Autowired
  public PlayerRankService(
      RiotRankPort riotRankPort,
      PlayerRepository playerRepository,
      PlayerRankRepository playerRankRepository,
      Clock clock) {
    this.riotRankPort = riotRankPort;
    this.playerRepository = playerRepository;
    this.playerRankRepository = playerRankRepository;
    this.clock = clock;
  }

  public Optional<PlayerRankSnapshot> refreshRank(PlayerEntity player, String puuid) {
    return refreshRankAvailability(player, puuid).snapshot();
  }

  public RankRefreshResult refreshRankAvailability(PlayerEntity player, String puuid) {
    return refreshRankAvailability(player, puuid, null);
  }

  public RankRefreshResult refreshRankAvailabilityForQueue(
      PlayerEntity player, String puuid, String preferredQueueType) {
    return refreshRankAvailability(player, puuid, normalizeQueueType(preferredQueueType).orElse(null));
  }

  private RankRefreshResult refreshRankAvailability(
      PlayerEntity player, String puuid, String preferredQueueType) {
    if (puuid == null || puuid.isBlank()) {
      return storedRank(player, preferredQueueType)
          .map(RankRefreshResult::stored)
          .orElseGet(RankRefreshResult::noRank);
    }
    try {
      List<PlayerRankSnapshot> snapshots =
          toSnapshots(
              riotRankPort.fetchRankEntries(RiotPlatform.fromFormValue(player.getPlatform()), puuid));
      saveQueueSnapshots(player, snapshots);
      Optional<PlayerRankSnapshot> primarySnapshot = selectBestRank(snapshots);
      saveSnapshot(player, primarySnapshot.orElse(null));
      Optional<PlayerRankSnapshot> selectedSnapshot =
          selectRankForQueue(snapshots, preferredQueueType).or(() -> primarySnapshot);
      if (preferredQueueType != null) {
        selectedSnapshot = selectRankForQueue(snapshots, preferredQueueType);
      }
      return selectedSnapshot.map(RankRefreshResult::current).orElseGet(RankRefreshResult::unranked);
    } catch (RuntimeException e) {
      log.warn("Rank refresh failed for {}#{}", player.getGameName(), player.getTagLine(), e);
      return storedRank(player, preferredQueueType)
          .map(RankRefreshResult::refreshErrorWithStored)
          .orElseGet(RankRefreshResult::refreshErrorWithoutStored);
    }
  }

  @Transactional(readOnly = true)
  public Optional<PlayerRankSnapshot> rosterAverageRank() {
    List<Integer> scores =
        playerRepository.findAllByActiveTrueAndArchivedAtIsNullOrderByGameNameAsc().stream()
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
    return storedRank(player, null);
  }

  public Optional<PlayerRankSnapshot> storedRank(PlayerEntity player, String preferredQueueType) {
    Optional<String> normalizedQueue = normalizeQueueType(preferredQueueType);
    if (normalizedQueue.isPresent()) {
      Optional<PlayerRankSnapshot> storedSpecific = storedRankSnapshot(player, normalizedQueue.get());
      if (storedSpecific.isPresent()) {
        return storedSpecific;
      }
      if (!normalizedQueue.get().equals(player.getRankQueueType())) {
        return Optional.empty();
      }
    }
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

  @Transactional
  void saveSnapshot(PlayerEntity player, PlayerRankSnapshot snapshot) {
    PlayerEntity target =
        player.getId() == null ? player : playerRepository.findById(player.getId()).orElse(player);
    applySnapshot(target, snapshot);
    playerRepository.save(target);
  }

  @Transactional
  void saveQueueSnapshots(PlayerEntity player, List<PlayerRankSnapshot> snapshots) {
    if (playerRankRepository == null || player.getId() == null) {
      return;
    }
    PlayerEntity target =
        player.getId() == null ? player : playerRepository.findById(player.getId()).orElse(player);
    Map<String, PlayerRankSnapshot> snapshotByQueue = new HashMap<>();
    snapshots.forEach(snapshot -> snapshotByQueue.put(snapshot.queueType(), snapshot));
    List<PlayerRankEntity> existing =
        playerRankRepository.findAllByPlayerIdAndQueueTypeIn(player.getId(), SUPPORTED_RANK_QUEUES);
    Map<String, PlayerRankEntity> existingByQueue = new HashMap<>();
    existing.forEach(entity -> existingByQueue.put(entity.getQueueType(), entity));
    existing.stream()
        .filter(entity -> !snapshotByQueue.containsKey(entity.getQueueType()))
        .forEach(playerRankRepository::delete);
    snapshotByQueue.values().forEach(snapshot -> saveQueueSnapshot(target, snapshot, existingByQueue));
  }

  private void saveQueueSnapshot(
      PlayerEntity player, PlayerRankSnapshot snapshot, Map<String, PlayerRankEntity> existingByQueue) {
    PlayerRankEntity entity = existingByQueue.getOrDefault(snapshot.queueType(), new PlayerRankEntity());
    entity.setPlayer(player);
    entity.setQueueType(snapshot.queueType());
    entity.setTier(snapshot.tier());
    entity.setDivision(snapshot.division());
    entity.setLeaguePoints(snapshot.leaguePoints());
    entity.setScore(snapshot.score());
    entity.setUpdatedAt(now());
    playerRankRepository.save(entity);
  }

  private List<PlayerRankSnapshot> toSnapshots(List<RiotRankEntry> entries) {
    return entries.stream()
        .filter(entry -> SUPPORTED_RANK_QUEUES.contains(entry.queueType()))
        .map(this::toSnapshot)
        .toList();
  }

  private PlayerRankSnapshot toSnapshot(RiotRankEntry entry) {
    return new PlayerRankSnapshot(
        entry.queueType(),
        entry.tier(),
        normalizeDivision(entry.rank()),
        entry.leaguePoints(),
        score(entry.tier(), entry.rank(), entry.leaguePoints()));
  }

  private Optional<PlayerRankSnapshot> selectBestRank(List<PlayerRankSnapshot> snapshots) {
    return snapshots.stream().min(Comparator.comparingInt(this::queuePriority));
  }

  private Optional<PlayerRankSnapshot> selectRankForQueue(
      List<PlayerRankSnapshot> snapshots, String preferredQueueType) {
    return normalizeQueueType(preferredQueueType)
        .flatMap(
            normalizedQueue ->
                snapshots.stream()
                    .filter(snapshot -> normalizedQueue.equals(snapshot.queueType()))
                    .findFirst());
  }

  private Optional<PlayerRankSnapshot> storedRankSnapshot(PlayerEntity player, String queueType) {
    if (playerRankRepository == null || player.getId() == null) {
      return Optional.empty();
    }
    return playerRankRepository
        .findByPlayerIdAndQueueType(player.getId(), queueType)
        .map(
            entity ->
                new PlayerRankSnapshot(
                    entity.getQueueType(),
                    entity.getTier(),
                    entity.getDivision(),
                    entity.getLeaguePoints(),
                    entity.getScore()));
  }

  private int queuePriority(PlayerRankSnapshot snapshot) {
    return SOLO_QUEUE.equals(snapshot.queueType()) ? 0 : 1;
  }

  private void applySnapshot(PlayerEntity player, PlayerRankSnapshot snapshot) {
    if (snapshot == null) {
      player.setRankQueueType(null);
      player.setRankTier(null);
      player.setRankDivision(null);
      player.setRankLeaguePoints(null);
      player.setRankScore(null);
      player.setRankUpdatedAt(now());
      return;
    }
    player.setRankQueueType(snapshot.queueType());
    player.setRankTier(snapshot.tier());
    player.setRankDivision(snapshot.division());
    player.setRankLeaguePoints(snapshot.leaguePoints());
    player.setRankScore(snapshot.score());
    player.setRankUpdatedAt(now());
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

  private Optional<String> normalizeQueueType(String queueType) {
    if (queueType == null || queueType.isBlank()) {
      return Optional.empty();
    }
    String normalized = queueType.trim().toUpperCase();
    return SUPPORTED_RANK_QUEUES.contains(normalized) ? Optional.of(normalized) : Optional.empty();
  }

  private java.time.Instant now() {
    return clock == null ? java.time.Instant.now() : clock.instant();
  }

  public record RankRefreshResult(
      Optional<PlayerRankSnapshot> snapshot, RankAvailability availability) {

    public static RankRefreshResult current(PlayerRankSnapshot snapshot) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.CURRENT);
    }

    public static RankRefreshResult stored(PlayerRankSnapshot snapshot) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.STORED);
    }

    public static RankRefreshResult unranked() {
      return new RankRefreshResult(Optional.empty(), RankAvailability.UNRANKED);
    }

    public static RankRefreshResult noRank() {
      return new RankRefreshResult(Optional.empty(), RankAvailability.NO_RANK);
    }

    public static RankRefreshResult refreshErrorWithStored(PlayerRankSnapshot snapshot) {
      return new RankRefreshResult(Optional.of(snapshot), RankAvailability.REFRESH_ERROR_STORED);
    }

    public static RankRefreshResult refreshErrorWithoutStored() {
      return new RankRefreshResult(Optional.empty(), RankAvailability.REFRESH_ERROR_NO_STORED);
    }
  }

  public enum RankAvailability {
    CURRENT,
    STORED,
    UNRANKED,
    NO_RANK,
    REFRESH_ERROR_STORED,
    REFRESH_ERROR_NO_STORED
  }
}
