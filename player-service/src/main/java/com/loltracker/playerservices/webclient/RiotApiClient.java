package com.loltracker.playerservices.webclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RiotApiClient {

  private final WebClient webClient;

  public RiotApiClient(
      @Value("${riot.api.key}") String apiKey, @Value("${riot.api.base-url}") String baseUrl) {
    this.webClient =
        WebClient.builder().baseUrl(baseUrl).defaultHeader("X-Riot-Token", apiKey).build();
  }

  public Mono<String> getSummonerByNameAndTagLine(String summonerName, String tagLine) {
    return webClient
        .get()
        .uri("riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}", summonerName, tagLine)
        .retrieve()
        .bodyToMono(String.class);
  }

  public Mono<String> getMatchesByPuuid(String puuid) {
    return webClient
        .get()
        .uri("riot/lol/match/v5/matches/by-puuid/{puuid}/idss", puuid)
        .retrieve()
        .bodyToMono(String.class);
  }
}
