package com.loltracker.playerservices.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loltracker.playerservices.domain.services.PlayerDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerServiceUseCaseTest {

  private PlayerServiceUseCase playerServiceUseCase;

  @Mock private PlayerDataService playerDataService;

  @BeforeEach
  void setUp() {
    playerServiceUseCase = new PlayerServiceUseCase(playerDataService);
  }

  @Test
  void loadPlayers() {
    playerServiceUseCase.loadPlayers();
    verify(playerDataService).refreshPlayerData();
  }

  @Test
  void scheduleReactiveTask_DoesNotThrow() {
    playerServiceUseCase.scheduleReactiveTask();
    assertTrue(true);
  }
}
