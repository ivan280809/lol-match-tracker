package com.loltracker.app.integration.riot;

import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RiotClient {

  private final WebClient.Builder webClientBuilder;
  private final ObjectMapper objectMapper;
  private final AppConfigurationService appConfigurationService;

  public RiotAccount fetchAccount(String gameName, String tagLine) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    String body;
    try {
      body =
          riotClient(configuration)
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
    RuntimeAppConfiguration configuration = riotConfiguration();
    String body =
        riotClient(configuration)
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
    RuntimeAppConfiguration configuration = riotConfiguration();
    String body =
        riotClient(configuration)
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

  private RuntimeAppConfiguration riotConfiguration() {
    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();
    if (configuration.riotApiKey() == null || configuration.riotApiKey().isBlank()) {
      throw new IllegalStateException("Riot API key is not configured");
    }
    return configuration;
  }

  private WebClient riotClient(RuntimeAppConfiguration configuration) {
    return webClientBuilder
        .baseUrl(configuration.riotRegion().baseUrl())
        .defaultHeader("X-Riot-Token", configuration.riotApiKey())
        .build();
  }
}
