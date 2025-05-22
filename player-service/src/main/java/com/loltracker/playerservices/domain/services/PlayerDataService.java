package com.loltracker.playerservices.domain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.models.PlayerJson;
import com.loltracker.playerservices.infrastructure.models.AccountMatchesDTO;
import com.loltracker.playerservices.infrastructure.models.account.AccountDTO;
import com.loltracker.playerservices.infrastructure.models.matches.MatchDto;
import com.loltracker.playerservices.infrastructure.models.matches.MatchesDTO;
import com.loltracker.playerservices.infrastructure.webclients.MatchServiceWebClient;
import com.loltracker.playerservices.infrastructure.webclients.RiotApiClient;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
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
    Flux.fromIterable(players)
        .concatMap(
            player ->
                getSummonerData(player.getSummonerName(), player.getTagLine())
                    .doOnSubscribe(
                        s ->
                            log.info(
                                "Subscribed to getSummonerData for {}", player.getSummonerName()))
                    .doOnNext(r -> log.info("putMatches OK -> {}", r))
                    .doOnError(e -> log.error("putMatches FAILED", e))
                    .doOnSuccess(
                        result ->
                            System.out.println(
                                "Summoner data refreshed for " + player.getSummonerName()))
                    .onErrorResume(
                        error -> {
                          System.err.println(
                              "Error refreshing data for "
                                  + player.getSummonerName()
                                  + ": "
                                  + error.getMessage());
                          return Mono.empty();
                        })
                    .delaySubscription(Duration.ofSeconds(20)))
        .subscribe();
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
      List<String> matchIds =
          objectMapper.readValue(response, new TypeReference<List<String>>() {});
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
      log.error("Error occurred during summoner data fetching: ", e);
      return Mono.error(new RuntimeException("User not found " + e));
    };
  }
}
