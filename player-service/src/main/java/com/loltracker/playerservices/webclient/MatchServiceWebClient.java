package com.loltracker.playerservices.webclient;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class MatchServiceWebClient {

  private final WebClient webClient;

  public MatchServiceWebClient(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl("http://localhost:8084").build();
  }

  public Mono<String> putMatches(String puuid, Mono<MatchesDTO> matches) {
    return matches.flatMap(
        matchesDTO ->
            webClient
                .post()
                .uri("/match-history/createMatchHistory/{puuid}", puuid)
                .bodyValue(matchesDTO)
                .retrieve()
                .bodyToMono(String.class));
  }
}
