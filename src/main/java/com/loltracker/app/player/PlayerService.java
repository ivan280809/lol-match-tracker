package com.loltracker.app.player;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotClient;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private final PlayerRepository playerRepository;
  private final RiotClient riotClient;

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

    PlayerEntity entity = new PlayerEntity();
    entity.setGameName(gameName);
    entity.setTagLine(tagLine);
    entity.setPlatform(platform);
    entity.setPuuid(puuid);
    entity.setActive(form.active());
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
      if (riotClient.isConfigured()) {
        entity.setPuuid(resolvePuuid(gameName, tagLine));
      } else if (identityChanged) {
        entity.setPuuid(null);
      }
    }
    entity.setGameName(gameName);
    entity.setTagLine(tagLine);
    entity.setPlatform(platform);
    entity.setActive(form.active());
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
      entity.setArchivedAt(Instant.now());
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
    Instant now = Instant.now();
    entity.setPuuid(puuid);
    entity.setLastPolledAt(now);
    entity.setLastSuccessfulSyncAt(now);
    entity.setLastSyncStatus("SUCCESS");
    entity.setLastError(null);
    playerRepository.save(entity);
  }

  @Transactional
  public void updateSyncFailure(PlayerEntity player, String error) {
    PlayerEntity entity = getRequiredPlayer(player.getId());
    entity.setLastPolledAt(Instant.now());
    entity.setLastSyncStatus("ERROR");
    entity.setLastError(error == null ? "Unknown error" : error.substring(0, Math.min(500, error.length())));
    playerRepository.save(entity);
  }

  public String ensurePuuid(PlayerEntity player) {
    if (player.getPuuid() != null && !player.getPuuid().isBlank()) {
      return player.getPuuid();
    }

    String puuid = resolvePuuid(player.getGameName(), player.getTagLine());
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
    RiotAccount account = riotClient.fetchAccount(gameName, tagLine);
    return account.puuid();
  }

  private String resolvePuuidIfConfigured(String gameName, String tagLine) {
    if (!riotClient.isConfigured()) {
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

  private String normalize(String value) {
    return value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
