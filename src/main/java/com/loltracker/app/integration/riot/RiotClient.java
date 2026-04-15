package com.loltracker.app.integration.riot;

import com.loltracker.app.match.MatchSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RiotClient {

  private final WebClient.Builder webClientBuilder;
  private final ObjectMapper objectMapper;

  @Value("${riot.api.base-url}")
  private String baseUrl;

  @Value("${riot.api.key}")
  private String apiKey;

  public RiotAccount fetchAccount(String gameName, String tagLine) {
    String body;
    try {
      body =
          webClientBuilder
              .baseUrl(baseUrl)
              .defaultHeader("X-Riot-Token", apiKey)
              .build()
              .get()
              .uri("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}", gameName, tagLine)
              .retrieve()
              .bodyToMono(String.class)
              .block();
    } catch (WebClientResponseException.NotFound e) {
      throw new IllegalArgumentException(
          "Riot account not found for %s#%s".formatted(gameName, tagLine), e);
    }

    try {
      JsonNode node = objectMapper.readTree(body);
      return new RiotAccount(
          node.path("puuid").asText(), node.path("gameName").asText(), node.path("tagLine").asText());
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Riot account response", e);
    }
  }

  public List<String> fetchRecentMatchIds(String puuid) {
    String body =
        webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("X-Riot-Token", apiKey)
            .build()
            .get()
            .uri("/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count=10", puuid)
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      JsonNode node = objectMapper.readTree(body);
      List<String> matchIds = new ArrayList<>();
      node.forEach(item -> matchIds.add(item.asText()));
      return matchIds;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Riot match id response", e);
    }
  }

  public MatchSummary fetchMatchSummary(String matchId, String puuid) {
    String body =
        webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("X-Riot-Token", apiKey)
            .build()
            .get()
            .uri("/lol/match/v5/matches/{matchId}", matchId)
            .retrieve()
            .bodyToMono(String.class)
            .block();

    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode metadata = root.path("metadata");
      JsonNode info = root.path("info");
      JsonNode participant = findParticipant(info.path("participants"), puuid);
      if (participant == null) {
        throw new IllegalStateException("Participant not found in Riot response");
      }
      return new MatchSummary(
          metadata.path("matchId").asText(matchId),
          participant.path("championName").asText("Unknown"),
          participant.path("win").asBoolean(false),
          info.path("gameMode").asText("Unknown"),
          info.path("gameDuration").asLong(0),
          Instant.ofEpochMilli(info.path("gameEndTimestamp").asLong(0)));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Riot match response", e);
    }
  }

  private JsonNode findParticipant(JsonNode participants, String puuid) {
    for (JsonNode participant : participants) {
      if (puuid.equals(participant.path("puuid").asText())) {
        return participant;
      }
    }
    return null;
  }
}
