package com.loltracker.playerservices.domain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.models.UserGameHeader;
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
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        return processUser(response, objectMapper);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private static Mono<String> processUser(String response, ObjectMapper objectMapper)
      throws JsonProcessingException {
    UserGameHeader userGameHeader = objectMapper.readValue(response, UserGameHeader.class);
    return Mono.just(userGameHeader.toString());
  }

  private Function<Throwable, Mono<? extends String>> handleError() {
    return e -> {
      return Mono.error(new RuntimeException("Error fetching summoner data", e));
    };
  }
}
