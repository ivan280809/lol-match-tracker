package com.loltracker.playerservices.domain.services;

import com.loltracker.playerservices.webclient.RiotApiClient;
import java.util.function.Function;
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
        .flatMap(handleResponse())
        .onErrorResume(handleError());
  }

  private Function<String, Mono<? extends String>> handleResponse() {
    return response -> {
      return Mono.just(response);
    };
  }

  private Function<Throwable, Mono<? extends String>> handleError() {
    return e -> {
      return Mono.error(new RuntimeException("Error fetching summoner data", e));
    };
  }
}
