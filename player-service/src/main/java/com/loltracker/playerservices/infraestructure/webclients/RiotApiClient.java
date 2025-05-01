package com.loltracker.playerservices.infraestructure.webclients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
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
        .bodyToMono(String.class)
        .doOnSubscribe(s -> log.info("Fetching summoner: {} with tag: {}", summonerName, tagLine))
        .doOnNext(response -> log.info("Response for summoner: {}: {}", summonerName, response))
        .doOnError(
            response ->
                log.info("Error fetching summoner: {} with tag: {}", summonerName, tagLine));
  }

  public Mono<String> getMatchesByPuuid(String puuid) {
    return webClient
        .get()
        .uri("lol/match/v5/matches/by-puuid/{puuid}/ids", puuid)
        .retrieve()
        .bodyToMono(String.class)
        .doOnSubscribe(s -> log.info("Fetching Data getMatchesByPuuid with puuid: {} ", puuid))
        .doOnNext(response -> log.info("Response for puuid: {}: {}", puuid, response));
  }

  public Mono<String> getMatchById(String matchId) {
    return webClient
        .get()
        .uri("/lol/match/v5/matches/{matchId}", matchId)
        .retrieve()
        .bodyToMono(String.class)
        .doOnSubscribe(s -> log.info("getMatchById: {}", matchId))
        .doOnNext(response -> log.info("Response for getMatchById: {}: {}", matchId, response));
  }
}
