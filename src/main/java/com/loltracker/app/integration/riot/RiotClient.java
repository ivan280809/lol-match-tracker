package com.loltracker.app.integration.riot;

import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.player.RiotPlatform;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RiotClient {

  private final WebClient.Builder webClientBuilder;
  private final ObjectMapper objectMapper;
  private final AppConfigurationService appConfigurationService;

  @Value("${app.http.timeout:PT10S}")
  private Duration httpTimeout;

  @Value("${app.http.retry.max-attempts:3}")
  private int httpMaxAttempts;

  @Value("${app.http.retry.backoff:PT1S}")
  private Duration httpRetryBackoff;

  public boolean isConfigured() {
    return appConfigurationService.getView().riotApiKeyConfigured();
  }

  public RiotAccount fetchAccount(String gameName, String tagLine) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    String body;
    try {
      body =
          blockWithRetry(
              riotClient(configuration)
                  .get()
                  .uri(
                      "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}",
                      gameName,
                      tagLine)
                  .retrieve()
                  .bodyToMono(String.class));
    } catch (RuntimeException e) {
      throw classifyFailure(e, "No se pudo validar la cuenta Riot %s#%s".formatted(gameName, tagLine));
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
    String body;
    try {
      body =
          blockWithRetry(
              riotClient(configuration)
                  .get()
                  .uri("/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count=10", puuid)
                  .retrieve()
                  .bodyToMono(String.class));
    } catch (RuntimeException e) {
      throw classifyFailure(e, "No se pudo consultar el historial reciente de Riot");
    }

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
    String body;
    try {
      body =
          blockWithRetry(
              riotClient(configuration)
                  .get()
                  .uri("/lol/match/v5/matches/{matchId}", matchId)
                  .retrieve()
                  .bodyToMono(String.class));
    } catch (RuntimeException e) {
      throw classifyFailure(e, "No se pudo consultar el detalle de partida Riot");
    }

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

  public List<RiotRankEntry> fetchRankEntries(RiotPlatform platform, String puuid) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    String summonerBody;
    try {
      summonerBody =
          blockWithRetry(
              platformClient(configuration, platform)
                  .get()
                  .uri("/lol/summoner/v4/summoners/by-puuid/{puuid}", puuid)
                  .retrieve()
                  .bodyToMono(String.class));
    } catch (RuntimeException e) {
      throw classifyFailure(e, "No se pudo consultar el invocador de Riot");
    }

    String summonerId;
    try {
      JsonNode node = objectMapper.readTree(summonerBody);
      summonerId = node.path("id").asText();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Riot summoner response", e);
    }

    String leagueBody;
    try {
      leagueBody =
          blockWithRetry(
              platformClient(configuration, platform)
                  .get()
                  .uri("/lol/league/v4/entries/by-summoner/{summonerId}", summonerId)
                  .retrieve()
                  .bodyToMono(String.class));
    } catch (RuntimeException e) {
      throw classifyFailure(e, "No se pudo consultar el rank de Riot");
    }

    try {
      JsonNode node = objectMapper.readTree(leagueBody);
      List<RiotRankEntry> entries = new ArrayList<>();
      node.forEach(
          item ->
              entries.add(
                  new RiotRankEntry(
                      item.path("queueType").asText(),
                      item.path("tier").asText(),
                      item.path("rank").asText(),
                      item.path("leaguePoints").asInt(0),
                      item.path("wins").asInt(0),
                      item.path("losses").asInt(0))));
      return entries;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Riot league response", e);
    }
  }

  public String validateApiKey(RiotPlatform platform) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    try {
      String body =
          blockWithRetry(
              platformClient(configuration, RiotPlatform.fromFormValue(platform))
                  .get()
                  .uri("/lol/status/v4/platform-data")
                  .retrieve()
                  .bodyToMono(String.class));
      JsonNode node = objectMapper.readTree(body);
      String name = node.path("name").asText(RiotPlatform.fromFormValue(platform).displayName());
      return "Riot OK en " + name;
    } catch (RuntimeException e) {
      throw classifyFailure(e, "No se pudo validar la Riot API key");
    } catch (Exception e) {
      throw new RiotApiException(
          RiotErrorCategory.UNKNOWN, "Riot respondio, pero no se pudo interpretar la respuesta", e);
    }
  }

  private RuntimeAppConfiguration riotConfiguration() {
    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();
    if (configuration.riotApiKey() == null || configuration.riotApiKey().isBlank()) {
      throw new RiotApiException(RiotErrorCategory.NOT_CONFIGURED, "Riot API key no configurada");
    }
    return configuration;
  }

  private WebClient riotClient(RuntimeAppConfiguration configuration) {
    return webClientBuilder
        .baseUrl(configuration.riotRegion().baseUrl())
        .defaultHeader("X-Riot-Token", configuration.riotApiKey())
        .build();
  }

  private WebClient platformClient(RuntimeAppConfiguration configuration, RiotPlatform platform) {
    return webClientBuilder
        .baseUrl(platform.baseUrl())
        .defaultHeader("X-Riot-Token", configuration.riotApiKey())
        .build();
  }

  private String blockWithRetry(Mono<String> response) {
    return response
        .timeout(httpTimeout)
        .retryWhen(
            Retry.backoff(Math.max(0, httpMaxAttempts - 1), httpRetryBackoff)
                .filter(this::isTransientFailure)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
        .block();
  }

  private boolean isTransientFailure(Throwable failure) {
    if (failure instanceof TimeoutException || failure instanceof WebClientRequestException) {
      return true;
    }
    if (failure instanceof WebClientResponseException exception) {
      return exception.getStatusCode().is5xxServerError();
    }
    return false;
  }

  private RiotApiException classifyFailure(RuntimeException failure, String fallbackMessage) {
    if (failure instanceof RiotApiException riotApiException) {
      return riotApiException;
    }
    if (failure instanceof WebClientResponseException exception) {
      int status = exception.getStatusCode().value();
      if (status == 401 || status == 403) {
        return new RiotApiException(
            RiotErrorCategory.UNAUTHORIZED, "Riot API key no autorizada o caducada", failure);
      }
      if (status == 404) {
        return new RiotApiException(RiotErrorCategory.NOT_FOUND, "Cuenta Riot no encontrada", failure);
      }
      if (status == 429) {
        return new RiotApiException(RiotErrorCategory.RATE_LIMIT, "Riot rate limit alcanzado", failure);
      }
      if (exception.getStatusCode().is5xxServerError()) {
        return new RiotApiException(
            RiotErrorCategory.UNAVAILABLE, "Riot no esta disponible temporalmente", failure);
      }
    }
    if (failure.getCause() instanceof TimeoutException) {
      return new RiotApiException(RiotErrorCategory.TIMEOUT, "Timeout llamando a Riot", failure);
    }
    return new RiotApiException(RiotErrorCategory.UNKNOWN, fallbackMessage, failure);
  }

}
