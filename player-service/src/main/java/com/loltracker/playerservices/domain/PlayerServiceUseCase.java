package com.loltracker.playerservices.domain;

import com.loltracker.playerservices.domain.services.PlayerDataService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@EnableScheduling
public class PlayerServiceUseCase {

  private final PlayerDataService playerDataService;

  @PostConstruct
  public void loadPlayers() {
    playerDataService.loadPlayers();
  }

  @Scheduled(fixedRate = 1000 * 60)
  public void refreshPlayerData() {
    playerDataService.refreshPlayerData();
  }

  public Mono<String> getSummonerData(String summonerName, String tagLine) {
    return playerDataService.getSummonerData(summonerName, tagLine);
  }
}
