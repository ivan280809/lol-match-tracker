package com.loltracker.playerservices.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loltracker.playerservices.domain.services.PlayerDataService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.scheduler.VirtualTimeScheduler;

@ExtendWith(SpringExtension.class)
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

    verify(playerDataService).loadPlayers();
    verify(playerDataService).refreshPlayerData();
  }

  @Test
  void scheduleReactiveTask() {
    VirtualTimeScheduler.getOrSet();

    playerServiceUseCase.scheduleReactiveTask();

    verify(playerDataService, never()).refreshPlayerData();

    VirtualTimeScheduler.get().advanceTimeBy(Duration.ofMinutes(5));

    verify(playerDataService, atLeastOnce()).refreshPlayerData();
  }

  @Test
  void getSummonerData() {
    String name = "Foo";
    String tag = "Bar";
    Mono<String> expected = Mono.just("datos");
    when(playerDataService.getSummonerData(name, tag)).thenReturn(expected);

    Mono<String> result = playerServiceUseCase.getSummonerData(name, tag);

    assertSame(expected, result);
    verify(playerDataService).getSummonerData(name, tag);
  }
}
