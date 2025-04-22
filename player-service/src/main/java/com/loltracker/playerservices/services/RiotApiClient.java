package com.loltracker.playerservices.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class RiotApiClient {

  private final WebClient webClient;
  private final String apiKey;
  private final String baseUrl;

  public RiotApiClient(
      @Value("${riot.api.key}") String apiKey, @Value("${riot.api.base-url}") String baseUrl) {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.webClient =
        WebClient.builder().baseUrl(baseUrl).defaultHeader("X-Riot-Token", apiKey).build();
  }

  public Mono<String> getSummonerByName(String summonerName) {
    return webClient
        .get()
        .uri("/lol/summoner/v4/summoners/by-name/{summonerName}", summonerName)
        .retrieve()
        .bodyToMono(String.class); // Luego definimos DTOs
  }
}
