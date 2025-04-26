package com.loltracker.playerservices.domain;

import com.loltracker.playerservices.domain.services.PlayerDataService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class PlayerServiceUseCase {

  private final PlayerDataService playerDataService;

  public Mono<String> getSummonerData(String summonerName, String tagLine) {
    return playerDataService.getSummonerData(summonerName, tagLine);
  }
}
