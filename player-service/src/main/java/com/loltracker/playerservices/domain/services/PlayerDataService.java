package com.loltracker.playerservices.domain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.exceptions.UserNotFoundException;
import com.loltracker.playerservices.domain.models.Account;
import com.loltracker.playerservices.domain.models.PlayerJson;
import com.loltracker.playerservices.infraestructure.models.AccountMatchesDTO;
import com.loltracker.playerservices.infraestructure.models.account.AccountDTO;
import com.loltracker.playerservices.infraestructure.models.matches.MatchDto;
import com.loltracker.playerservices.infraestructure.models.matches.MatchesDTO;
import com.loltracker.playerservices.infraestructure.webclients.MatchServiceWebClient;
import com.loltracker.playerservices.infraestructure.webclients.RiotApiClient;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class PlayerDataService {

  private final RiotApiClient riotApiClient;
  private final MatchServiceWebClient matchServiceWebClient;
  private final ObjectMapper objectMapper;
  private List<PlayerJson> players;

  public void loadPlayers() {
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("players.json");
      players = objectMapper.readValue(inputStream, new TypeReference<List<PlayerJson>>() {});
    } catch (Exception e) {
      throw new RuntimeException("Failed to load players.json", e);
    }
  }

  public void refreshPlayerData() {
    for (PlayerJson player : players) {
      getSummonerData(player.getSummonerName(), player.getTagLine())
              .doOnSubscribe(s -> log.info("Subscribed to getSummonerData for {}", player.getSummonerName()))
              .doOnNext(r -> log.info("putMatches OK -> {}", r))
              .doOnError(e -> log.error("putMatches FAILED", e))
          .subscribe(
              result ->
                  System.out.println("Summoner data refreshed for " + player.getSummonerName()),
              error ->
                  System.err.println(
                      "Error refreshing data for "
                          + player.getSummonerName()
                          + ": "
                          + error.getMessage()))
      ;
    }
  }

  public Mono<String> getSummonerData(String summonerName, String tagLine) {
    return riotApiClient
        .getSummonerByNameAndTagLine(summonerName, tagLine)
        .flatMap(handleSummonerDetails())
        .onErrorResume(handleError());
  }

  private Function<String, Mono<? extends String>> handleSummonerDetails() {
    return response -> {
      try {
        return processUser(response);
      } catch (JsonProcessingException e) {
        return Mono.error(e);
      }
    };
  }

  private Mono<String> processUser(String response) throws JsonProcessingException {
    log.info("Processing response: {}", response);
    AccountDTO accountDTO = objectMapper.readValue(response, AccountDTO.class);
    return getMatchesByPuuid(accountDTO)
        .map(matchesDTO -> new AccountMatchesDTO(accountDTO, matchesDTO))
        .flatMap(matchServiceWebClient::putMatches);
  }

  private Mono<MatchesDTO> getMatchesByPuuid(AccountDTO accountDTO) {
    return riotApiClient.getMatchesByPuuid(accountDTO.getPuuid()).flatMap(this::parseMatches);
  }

  private Mono<MatchesDTO> parseMatches(String response) {
    try {
      List<String> matchIds = objectMapper.readValue(response, new TypeReference<List<String>>() {});
      return Flux.fromIterable(matchIds)
              .flatMap(matchId -> riotApiClient.getMatchById(matchId).map(parseMatch()), 1)
              .collectList()
              .map(MatchesDTO::new);
    } catch (JsonProcessingException e) {
      return Mono.error(new RuntimeException("Error parsing MatchesIds", e));
    }
  }

  private Function<String, MatchDto> parseMatch() {
    return match -> {
      try {
        return objectMapper.readValue(match, MatchDto.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error parsing Match", e);
      }
    };
  }

  private Function<Throwable, Mono<? extends String>> handleError() {
    return e -> {
      log.error("Error occurred during summoner data fetching: ", e);  // Log detallado de errores
      return Mono.error(new RuntimeException("User not found " + e));
    };
  }
}
