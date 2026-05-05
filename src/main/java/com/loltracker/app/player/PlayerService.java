package com.loltracker.app.player;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotAccountPort;
import com.loltracker.app.match.MatchSummary;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private final PlayerRepository playerRepository;
  private final RiotAccountPort riotAccountPort;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<PlayerView> getAllPlayers() {
    return playerRepository.findAll().stream().map(PlayerView::fromEntity).toList();
  }

  @Transactional(readOnly = true)
  public List<PlayerEntity> getActivePlayers() {
    return playerRepository.findAllByActiveTrueAndArchivedAtIsNullOrderByGameNameAsc();
  }

  @Transactional(readOnly = true)
  public PlayerView getPlayer(Long id) {
    return PlayerView.fromEntity(getRequiredPlayer(id));
  }

  @Transactional(readOnly = true)
  public PlayerEntity getPlayerEntity(Long id) {
    return getRequiredPlayer(id);
  }

  public PlayerView create(PlayerForm form) {
    String gameName = normalize(form.gameName());
    String tagLine = normalize(form.tagLine());
    RiotPlatform platform = RiotPlatform.fromFormValue(form.platform());
    assertNoDuplicate(null, gameName, tagLine);
    String puuid = resolvePuuidIfConfigured(gameName, tagLine);
    assertNoPuuidDuplicate(null, puuid);

    PlayerEntity entity = new PlayerEntity();
    entity.setGameName(gameName);
    entity.setTagLine(tagLine);
    entity.setPlatform(platform);
    entity.setPuuid(puuid);
    entity.setActive(form.active());
    entity.setTrackFrom(now());
    entity.setBackfillMode(
        Boolean.TRUE.equals(form.backfill())
            ? PlayerBackfillMode.IMPORT_WITHOUT_NOTIFICATIONS
            : PlayerBackfillMode.NONE);
    return PlayerView.fromEntity(playerRepository.save(entity));
  }

  public PlayerView update(Long id, PlayerForm form) {
    PlayerEntity entity = getRequiredPlayer(id);
    String gameName = normalize(form.gameName());
    String tagLine = normalize(form.tagLine());
    RiotPlatform platform = RiotPlatform.fromFormValue(form.platform());
    assertNoDuplicate(id, gameName, tagLine);
    boolean identityChanged =
        !gameName.equalsIgnoreCase(entity.getGameName())
            || !tagLine.equalsIgnoreCase(entity.getTagLine());
    boolean platformChanged = platform != RiotPlatform.fromFormValue(entity.getPlatform());
    if (identityChanged || platformChanged || isBlank(entity.getPuuid())) {
      if (riotAccountPort.isConfigured()) {
        String puuid = resolvePuuid(gameName, tagLine);
        assertNoPuuidDuplicate(id, puuid);
        entity.setPuuid(puuid);
      } else if (identityChanged) {
        entity.setPuuid(null);
      }
    }
    entity.setGameName(gameName);
    entity.setTagLine(tagLine);
    entity.setPlatform(platform);
    entity.setActive(form.active());
    if (Boolean.TRUE.equals(form.backfill())) {
      entity.setBackfillMode(PlayerBackfillMode.IMPORT_WITHOUT_NOTIFICATIONS);
      if (entity.getTrackFrom() == null) {
        entity.setTrackFrom(now());
      }
    }
    return PlayerView.fromEntity(playerRepository.save(entity));
  }

  @Transactional
  public void setActive(Long id, boolean active) {
    PlayerEntity entity = getRequiredPlayer(id);
    entity.setActive(active);
    playerRepository.save(entity);
  }

  @Transactional
  public void archive(Long id) {
    PlayerEntity entity = getRequiredPlayer(id);
    if (entity.getArchivedAt() == null) {
      entity.setArchivedAt(now());
    }
    entity.setActive(false);
    playerRepository.save(entity);
  }

  @Transactional
  public void restore(Long id) {
    PlayerEntity entity = getRequiredPlayer(id);
    entity.setArchivedAt(null);
    entity.setActive(true);
    playerRepository.save(entity);
  }

  @Transactional
  public void updateSyncSuccess(PlayerEntity player, String puuid) {
    PlayerEntity entity = getRequiredPlayer(player.getId());
    Instant now = now();
    assertNoPuuidDuplicate(entity.getId(), puuid);
    entity.setPuuid(puuid);
    entity.setLastPolledAt(now);
    entity.setLastSuccessfulSyncAt(now);
    entity.setLastSyncStatus("SUCCESS");
    entity.setLastError(null);
    if (entity.getBackfillMode() == PlayerBackfillMode.IMPORT_WITHOUT_NOTIFICATIONS) {
      entity.setBackfillMode(PlayerBackfillMode.NONE);
    }
    playerRepository.save(entity);
  }

  @Transactional
  public void updateSyncFailure(PlayerEntity player, String error) {
    PlayerEntity entity = getRequiredPlayer(player.getId());
    entity.setLastPolledAt(now());
    entity.setLastSyncStatus("ERROR");
    entity.setLastError(error == null ? "Unknown error" : error.substring(0, Math.min(500, error.length())));
    playerRepository.save(entity);
  }

  public String ensurePuuid(PlayerEntity player) {
    if (player.getPuuid() != null && !player.getPuuid().isBlank()) {
      return player.getPuuid();
    }

    String puuid = resolvePuuid(player.getGameName(), player.getTagLine());
    assertNoPuuidDuplicate(player.getId(), puuid);
    player.setPuuid(puuid);
    playerRepository.save(player);
    return puuid;
  }

  @Transactional(readOnly = true)
  public long countPlayers() {
    return playerRepository.count();
  }

  private PlayerEntity getRequiredPlayer(Long id) {
    return playerRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Player not found"));
  }

  private String resolvePuuid(String gameName, String tagLine) {
    RiotAccount account = riotAccountPort.fetchAccount(gameName, tagLine);
    return account.puuid();
  }

  private String resolvePuuidIfConfigured(String gameName, String tagLine) {
    if (!riotAccountPort.isConfigured()) {
      return null;
    }
    return resolvePuuid(gameName, tagLine);
  }

  private void assertNoDuplicate(Long playerId, String gameName, String tagLine) {
    playerRepository
        .findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine)
        .ifPresent(
            existing -> {
              if (playerId == null || !playerId.equals(existing.getId())) {
                throw new IllegalArgumentException("Player already exists");
              }
            });
  }

  public boolean shouldImportMatch(PlayerEntity player, MatchSummary summary) {
    Instant trackFrom = player.getTrackFrom();
    if (trackFrom == null || summary.gameEndAt() == null || !summary.gameEndAt().isBefore(trackFrom)) {
      return true;
    }
    return player.getBackfillMode() == PlayerBackfillMode.IMPORT_WITHOUT_NOTIFICATIONS;
  }

  public boolean shouldNotifyMatch(PlayerEntity player, MatchSummary summary) {
    Instant trackFrom = player.getTrackFrom();
    if (summary.gameEndAt() == null) {
      return false;
    }
    return trackFrom == null || !summary.gameEndAt().isBefore(trackFrom);
  }

  private void assertNoPuuidDuplicate(Long playerId, String puuid) {
    if (puuid == null || puuid.isBlank()) {
      return;
    }
    playerRepository
        .findByPuuid(puuid)
        .ifPresent(
            existing -> {
              if (playerId == null || !playerId.equals(existing.getId())) {
                throw new IllegalArgumentException("Ya existe un jugador con el mismo PUUID Riot");
              }
            });
  }

  private String normalize(String value) {
    return value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private Instant now() {
    return clock == null ? Instant.now() : clock.instant();
  }
}
