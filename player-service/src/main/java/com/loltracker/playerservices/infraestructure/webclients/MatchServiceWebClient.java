package com.loltracker.playerservices.infraestructure.webclients;

import com.loltracker.playerservices.infraestructure.models.AccountMatchesDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class MatchServiceWebClient {

  private final WebClient webClient;

  public MatchServiceWebClient(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl("http://localhost:8084").build();
  }

  public Mono<String> putMatches(AccountMatchesDTO accountMatchesDTO) {
    return webClient
        .post()
        .uri("/match-history/createMatchHistory")
        .bodyValue(accountMatchesDTO)
        .retrieve()
        .bodyToMono(String.class);
  }
}
