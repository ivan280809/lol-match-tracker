package com.loltracker.playerservices.domain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.exceptions.UserNotFoundException;
import com.loltracker.playerservices.domain.models.LolPlayerHeader;
import com.loltracker.playerservices.infraestructure.models.MatchesDTO;
import com.loltracker.playerservices.infraestructure.webclients.MatchServiceWebClient;
import com.loltracker.playerservices.infraestructure.webclients.RiotApiClient;
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
