package com.loltracker.app.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotClient;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

  @Mock private PlayerRepository playerRepository;
  @Mock private RiotClient riotClient;

  @InjectMocks private PlayerService playerService;

  @Test
  void createRejectsDuplicatePlayer() {
    when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP"))
        .thenReturn(Optional.of(new PlayerEntity()));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> playerService.create(new PlayerForm("Bazaga", "ESP", true)));

    assertEquals("Player already exists", exception.getMessage());
    verify(playerRepository, never()).save(any());
  }

  @Test
  void createPersistsNormalizedPlayer() {
    when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP"))
        .thenReturn(Optional.empty());
    when(riotClient.isConfigured()).thenReturn(true);
    when(riotClient.fetchAccount("Bazaga", "ESP"))
        .thenReturn(new RiotAccount("puuid-1", "Bazaga", "ESP"));
    when(playerRepository.save(any(PlayerEntity.class)))
        .thenAnswer(
            invocation -> {
              PlayerEntity entity = invocation.getArgument(0);
              entity.setId(7L);
              return entity;
            });

    PlayerView created = playerService.create(new PlayerForm("Bazaga", "ESP", true));

    assertEquals(7L, created.id());
    assertEquals("Bazaga", created.gameName());
    assertEquals("ESP", created.tagLine());
    assertEquals(RiotPlatform.EUW1, created.platform());
    assertEquals("puuid-1", created.puuid());
    assertTrue(created.active());
  }

  @Test
  void createPersistsWithoutPuuidWhenRiotIsNotConfigured() {
    when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP"))
        .thenReturn(Optional.empty());
    when(riotClient.isConfigured()).thenReturn(false);
    when(playerRepository.save(any(PlayerEntity.class)))
        .thenAnswer(
            invocation -> {
              PlayerEntity entity = invocation.getArgument(0);
              entity.setId(7L);
              return entity;
            });

    PlayerView created = playerService.create(new PlayerForm("Bazaga", "ESP", true));

    assertNull(created.puuid());
    verify(riotClient, never()).fetchAccount(anyString(), anyString());
  }

  @Test
  void updateRejectsDuplicatePlayer() {
    PlayerEntity player = new PlayerEntity();
    player.setId(7L);
    when(playerRepository.findById(7L)).thenReturn(Optional.of(player));

    PlayerEntity existing = new PlayerEntity();
    existing.setId(8L);
    when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP"))
        .thenReturn(Optional.of(existing));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> playerService.update(7L, new PlayerForm("Bazaga", "ESP", true)));

    assertEquals("Player already exists", exception.getMessage());
    verify(playerRepository, never()).save(any());
  }

  @Test
  void createStoresSelectedPlatform() {
    when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP"))
        .thenReturn(Optional.empty());
    when(riotClient.isConfigured()).thenReturn(true);
    when(riotClient.fetchAccount("Bazaga", "ESP"))
        .thenReturn(new RiotAccount("puuid-1", "Bazaga", "ESP"));
    when(playerRepository.save(any(PlayerEntity.class)))
        .thenAnswer(
            invocation -> {
              PlayerEntity entity = invocation.getArgument(0);
              entity.setId(7L);
              return entity;
            });

    PlayerView created = playerService.create(new PlayerForm(RiotPlatform.NA1, "Bazaga", "ESP", true));

    assertEquals(RiotPlatform.NA1, created.platform());
  }

  @Test
  void updateResolvesPuuidWhenIdentityChangesAndRiotConfigured() {
    PlayerEntity player = new PlayerEntity();
    player.setId(7L);
    player.setGameName("Old");
    player.setTagLine("EUW");
    player.setPlatform(RiotPlatform.EUW1);
    player.setPuuid("old-puuid");
    when(playerRepository.findById(7L)).thenReturn(Optional.of(player));
    when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP"))
        .thenReturn(Optional.empty());
    when(riotClient.isConfigured()).thenReturn(true);
    when(riotClient.fetchAccount("Bazaga", "ESP"))
        .thenReturn(new RiotAccount("new-puuid", "Bazaga", "ESP"));
    when(playerRepository.save(player)).thenReturn(player);

    PlayerView updated = playerService.update(7L, new PlayerForm(RiotPlatform.EUW1, "Bazaga", "ESP", true));

    assertEquals("new-puuid", updated.puuid());
    verify(riotClient).fetchAccount("Bazaga", "ESP");
  }

  @Test
  void updateClearsPuuidWhenIdentityChangesAndRiotIsNotConfigured() {
    PlayerEntity player = new PlayerEntity();
    player.setId(7L);
    player.setGameName("Old");
    player.setTagLine("EUW");
    player.setPlatform(RiotPlatform.EUW1);
    player.setPuuid("old-puuid");
    when(playerRepository.findById(7L)).thenReturn(Optional.of(player));
    when(playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP"))
        .thenReturn(Optional.empty());
    when(riotClient.isConfigured()).thenReturn(false);
    when(playerRepository.save(player)).thenReturn(player);

    PlayerView updated = playerService.update(7L, new PlayerForm(RiotPlatform.EUW1, "Bazaga", "ESP", true));

    assertNull(updated.puuid());
    verify(riotClient, never()).fetchAccount(anyString(), anyString());
  }

  @Test
  void archiveMarksPlayerInactiveAndArchived() {
    PlayerEntity player = new PlayerEntity();
    player.setId(7L);
    player.setActive(true);
    when(playerRepository.findById(7L)).thenReturn(Optional.of(player));

    playerService.archive(7L);

    assertFalse(player.isActive());
    assertNotNull(player.getArchivedAt());
    verify(playerRepository).save(player);
  }

  @Test
  void restoreClearsArchiveAndActivatesPlayer() {
    PlayerEntity player = new PlayerEntity();
    player.setId(7L);
    player.setActive(false);
    player.setArchivedAt(java.time.Instant.parse("2026-05-05T12:00:00Z"));
    when(playerRepository.findById(7L)).thenReturn(Optional.of(player));

    playerService.restore(7L);

    assertTrue(player.isActive());
    assertNull(player.getArchivedAt());
    verify(playerRepository).save(player);
  }

  @Test
  void ensurePuuidReusesPersistedValue() {
    PlayerEntity player = new PlayerEntity();
    player.setPuuid("existing-puuid");

    String puuid = playerService.ensurePuuid(player);

    assertEquals("existing-puuid", puuid);
    verifyNoInteractions(riotClient);
    verify(playerRepository, never()).save(any());
  }

  @Test
  void ensurePuuidFetchesAndPersistsWhenMissing() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("Bazaga");
    player.setTagLine("ESP");
    when(riotClient.fetchAccount("Bazaga", "ESP"))
        .thenReturn(new RiotAccount("resolved-puuid", "Bazaga", "ESP"));

    String puuid = playerService.ensurePuuid(player);

    assertEquals("resolved-puuid", puuid);
    assertEquals("resolved-puuid", player.getPuuid());
    verify(playerRepository).save(player);
  }
}
