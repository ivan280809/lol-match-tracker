package com.loltracker.playerservices.infrastructure.webclients;

import com.loltracker.playerservices.infrastructure.models.AccountMatchesDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class MatchServiceWebClient {

  private final WebClient webClient;

  public MatchServiceWebClient(
      WebClient.Builder webClientBuilder, @Value("${match.service.base-url}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public Mono<String> putMatches(AccountMatchesDTO accountMatchesDTO) {
    log.info(
        "Sending account match data for player: {}",
        accountMatchesDTO.getAccountDTO().getGameName());
    return webClient
        .post()
        .uri("/match-history/createMatchHistory")
        .bodyValue(accountMatchesDTO)
        .retrieve()
        .bodyToMono(String.class);
  }

  public Mono<Boolean> matchExists(String puuid, String matchId) {
    log.info("Checking if match exists for puuid: {} and matchId: {}", puuid, matchId);
    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/match-history/puuid/{puuid}/matchId/{matchId}/exists")
                    .build(puuid, matchId))
        .retrieve()
        .bodyToMono(Boolean.class);
  }
}
