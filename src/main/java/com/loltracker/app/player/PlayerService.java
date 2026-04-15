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
    return playerRepository.findAllByActiveTrueOrderByGameNameAsc();
  }

  @Transactional(readOnly = true)
  public PlayerView getPlayer(Long id) {
    return PlayerView.fromEntity(getRequiredPlayer(id));
  }

  @Transactional
  public PlayerView create(PlayerForm form) {
    String gameName = normalize(form.gameName());
    String tagLine = normalize(form.tagLine());
    assertNoDuplicate(null, gameName, tagLine);

    PlayerEntity entity = new PlayerEntity();
    entity.setGameName(gameName);
    entity.setTagLine(tagLine);
    entity.setPuuid(resolvePuuid(gameName, tagLine));
    entity.setActive(form.active());
    return PlayerView.fromEntity(playerRepository.save(entity));
  }

  @Transactional
  public PlayerView update(Long id, PlayerForm form) {
    PlayerEntity entity = getRequiredPlayer(id);
    String gameName = normalize(form.gameName());
    String tagLine = normalize(form.tagLine());
    assertNoDuplicate(id, gameName, tagLine);
    entity.setGameName(gameName);
    entity.setTagLine(tagLine);
    entity.setPuuid(resolvePuuid(gameName, tagLine));
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
  public void updateSyncSuccess(PlayerEntity player, String puuid) {
    player.setPuuid(puuid);
    player.setLastPolledAt(Instant.now());
    player.setLastSuccessfulSyncAt(Instant.now());
    player.setLastSyncStatus("SUCCESS");
    player.setLastError(null);
    playerRepository.save(player);
  }

  @Transactional
  public void updateSyncFailure(PlayerEntity player, String error) {
    player.setLastPolledAt(Instant.now());
    player.setLastSyncStatus("ERROR");
    player.setLastError(error == null ? "Unknown error" : error.substring(0, Math.min(500, error.length())));
    playerRepository.save(player);
  }

  @Transactional
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
}
