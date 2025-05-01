package com.loltracker.playerservices.domain;

import com.loltracker.playerservices.domain.services.PlayerDataService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@AllArgsConstructor
@EnableScheduling
public class PlayerServiceUseCase {

  private final PlayerDataService playerDataService;

  @PostConstruct
  public void loadPlayers() {
    playerDataService.loadPlayers();
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

  public Mono<String> getSummonerData(String summonerName, String tagLine) {
    return playerDataService.getSummonerData(summonerName, tagLine);
  }
}
