package com.loltracker.playerservices.domain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.exceptions.UserNotFoundException;
import com.loltracker.playerservices.domain.models.LolPlayerHeader;
import com.loltracker.playerservices.domain.models.PlayerJson;
import com.loltracker.playerservices.infraestructure.models.matches.MatchesDTO;
import com.loltracker.playerservices.infraestructure.webclients.MatchServiceWebClient;
import com.loltracker.playerservices.infraestructure.webclients.RiotApiClient;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class PlayerDataService {

  private final RiotApiClient riotApiClient;
  private final MatchServiceWebClient matchServiceWebClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private List<PlayerJson> players;

  public void loadPlayers() {
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("players.json");
      players = objectMapper.readValue(inputStream, new TypeReference<List<PlayerJson>>() {});
    } catch (Exception e) {
      throw new RuntimeException("Failed to load players.json", e);
    }
  }

  public Mono<String> getSummonerData(String summonerName, String tagLine) {
    return riotApiClient
        .getSummonerByNameAndTagLine(summonerName, tagLine)
        .flatMap(handleSummonerDetails())
        .onErrorResume(handleError());
  }

  public void refreshPlayerData() {
    for (PlayerJson player : players) {
      getSummonerData(player.getSummonerName(), player.getTagLine())
          .subscribe(
              result -> {
                System.out.println("Summoner data refreshed for " + player.getSummonerName());
              },
              error -> {
                System.err.println(
                    "Error refreshing data for "
                        + player.getSummonerName()
                        + ": "
                        + error.getMessage());
              });
    }
  }

  private Function<String, Mono<? extends String>> handleSummonerDetails() {
    return response -> {
      try {
        return processUser(response);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private Mono<String> processUser(String response) throws JsonProcessingException {
    LolPlayerHeader lolPlayerHeader = objectMapper.readValue(response, LolPlayerHeader.class);
    Mono<MatchesDTO> matchesByPuuid = getMatchesByPuuid(lolPlayerHeader);
    return matchServiceWebClient.putMatches(lolPlayerHeader.getPuuid(), matchesByPuuid);
  }

  private Mono<MatchesDTO> getMatchesByPuuid(LolPlayerHeader lolPlayerHeader) {
    return riotApiClient.getMatchesByPuuid(lolPlayerHeader.getPuuid()).map(this::parseMatches);
  }

  private MatchesDTO parseMatches(String response) {
    try {
      List<String> matches = objectMapper.readValue(response, new TypeReference<List<String>>() {});
      return new MatchesDTO(matches);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error parsing MatchesIds", e);
    }
  }

  private Function<Throwable, Mono<? extends String>> handleError() {
    return e -> Mono.error(new UserNotFoundException("User not found " + e));
  }
}
