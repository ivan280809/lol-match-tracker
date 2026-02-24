package com.loltracker.playerservices.domain;

import com.loltracker.playerservices.domain.services.PlayerDataService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@EnableScheduling
public class PlayerServiceUseCase {

  private final PlayerDataService playerDataService;

  @PostConstruct
  public void loadPlayers() {
    refreshPlayerData();
  }

  @PostConstruct
  public void scheduleReactiveTask() {
    Flux.interval(Duration.ofMinutes(5))
        .flatMap(tick -> Mono.fromRunnable(this::refreshPlayerData))
        .subscribe();
  }

  private void refreshPlayerData() {
    playerDataService.refreshPlayerData();
  }
}
