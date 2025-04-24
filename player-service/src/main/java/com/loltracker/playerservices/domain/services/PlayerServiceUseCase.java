package com.loltracker.playerservices.domain.services;

import com.loltracker.playerservices.webclient.RiotApiClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class PlayerServiceUseCase {

  private final RiotApiClient riotApiClient;

  public Mono<String> getSummoner(String summonerName, String tagLine) {
    return riotApiClient
        .getSummonerByNameAndTagLine(summonerName, tagLine)
        .flatMap(
            response -> {
              return Mono.just(response);
            })
        .onErrorResume(
            e -> {
              return Mono.error(new RuntimeException("Error fetching summoner data", e));
            });
  }
}
