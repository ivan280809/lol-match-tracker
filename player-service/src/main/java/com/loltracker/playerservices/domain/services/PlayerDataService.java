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

  public void refreshPlayerData() {
    loadPlayers().concatMap(this::refreshDataForPlayer).subscribe();
  }

  private Flux<PlayerJson> loadPlayers() {
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("players.json");
      List<PlayerJson> players = objectMapper.readValue(inputStream, new TypeReference<>() {});
      return Flux.fromIterable(players);
    } catch (Exception e) {
      log.error("Failed to load players.json", e);
      return Flux.error(new RuntimeException("Failed to load players.json", e));
    }
  }

  private Mono<Void> refreshDataForPlayer(PlayerJson player) {
    return fetchAccountAndStoreMatches(player.getSummonerName(), player.getTagLine())
        .doOnSubscribe(s -> log.info("Fetching data for {}", player.getSummonerName()))
        .doOnNext(r -> log.info("putMatches OK -> {}", r))
        .doOnError(e -> log.error("putMatches FAILED for {}", player.getSummonerName(), e))
        .doOnSuccess(r -> log.info("Data refreshed for {}", player.getSummonerName()))
        .onErrorResume(
            e -> {
              log.warn("Skipping {}: {}", player.getSummonerName(), e.getMessage());
              return Mono.empty();
            })
        .delaySubscription(Duration.ofSeconds(20))
        .then();
  }

  Mono<String> fetchAccountAndStoreMatches(String summonerName, String tagLine) {
    return riotApiClient
        .getSummonerByNameAndTagLine(summonerName, tagLine)
        .flatMap(this::parseAndStoreAccountMatches)
        .onErrorResume(this::handleSummonerError);
  }

  private Mono<String> parseAndStoreAccountMatches(String response) {
    try {
      AccountDTO account = objectMapper.readValue(response, AccountDTO.class);
      return getFilteredMatchesForAccount(account)
          .map(matches -> new AccountMatchesDTO(account, matches))
          .flatMap(matchServiceWebClient::putMatches);
    } catch (JsonProcessingException e) {
      return Mono.error(new RuntimeException("Failed to parse account response", e));
    }
  }

  private Mono<MatchesDTO> getFilteredMatchesForAccount(AccountDTO account) {
    return riotApiClient
        .getMatchesByPuuid(account.getPuuid())
        .flatMap(matchesJson -> filterAndParseMatches(matchesJson, account.getPuuid()));
  }

  private Mono<MatchesDTO> filterAndParseMatches(String matchesJson, String puuid) {
    try {
      List<String> matchIds = objectMapper.readValue(matchesJson, new TypeReference<>() {});
      return Flux.fromIterable(matchIds)
          .filterWhen(
              matchId -> matchServiceWebClient.matchExists(puuid, matchId).map(exists -> !exists))
          .flatMap(matchId -> riotApiClient.getMatchById(matchId).map(this::toMatchDto), 1)
          .collectList()
          .map(MatchesDTO::new);
    } catch (JsonProcessingException e) {
      return Mono.error(new RuntimeException("Error parsing match IDs", e));
    }
  }

  private MatchDto toMatchDto(String matchJson) {
    try {
      return objectMapper.readValue(matchJson, MatchDto.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse match JSON", e);
    }
  }

  private Mono<String> handleSummonerError(Throwable e) {
    log.error("Error occurred during summoner data fetching", e);
    return Mono.error(new RuntimeException("Summoner data fetch failed", e));
  }
}
