package com.loltracker.app.integration.riot;

import com.loltracker.app.player.RiotPlatform;
import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RiotClient
    implements RiotAccountPort, RiotMatchPort, RiotRankPort, RiotOperationsPort {

  private static final Duration DEFAULT_RATE_LIMIT_PAUSE = Duration.ofMinutes(2);

  private final RestClient.Builder restClientBuilder;
  private final ObjectMapper objectMapper;
  private final AppConfigurationService appConfigurationService;
  private final Clock clock;
  private final OpsMetrics opsMetrics;

  @Value("${app.http.retry.max-attempts:3}")
  private int httpMaxAttempts;

  @Value("${app.http.retry.backoff:PT1S}")
  private Duration httpRetryBackoff;

  @Override
  public boolean isConfigured() {
    return appConfigurationService.getView().riotApiKeyConfigured();
  }

  @Override
  public RiotAccount fetchAccount(String gameName, String tagLine) {
    return observeRiot("account", () -> fetchAccountInternal(gameName, tagLine));
  }

  private RiotAccount fetchAccountInternal(String gameName, String tagLine) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    String body =
        executeWithRetry(
            () ->
                riotClient(configuration)
                    .get()
                    .uri(
                        "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}",
                        gameName,
                        tagLine)
                    .retrieve()
                    .body(String.class),
            "No se pudo validar la cuenta Riot %s#%s".formatted(gameName, tagLine));

    try {
      JsonNode node = objectMapper.readTree(body);
      String puuid = node.path("puuid").asText();
      if (puuid == null || puuid.isBlank()) {
        throw malformed("Riot respondio sin PUUID para la cuenta indicada", null);
      }
      return new RiotAccount(puuid, node.path("gameName").asText(gameName), node.path("tagLine").asText(tagLine));
    } catch (RiotApiException e) {
      throw e;
    } catch (Exception e) {
      throw malformed("Riot respondio con una cuenta mal formada", e);
    }
  }

  @Override
  public List<String> fetchRecentMatchIds(String puuid, int start, int count) {
    return observeRiot("match_ids", () -> fetchRecentMatchIdsInternal(puuid, start, count));
  }

  private List<String> fetchRecentMatchIdsInternal(String puuid, int start, int count) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    String body =
        executeWithRetry(
            () ->
                riotClient(configuration)
                    .get()
                    .uri(
                        "/lol/match/v5/matches/by-puuid/{puuid}/ids?start={start}&count={count}",
                        puuid,
                        Math.max(0, start),
                        Math.max(1, count))
                    .retrieve()
                    .body(String.class),
            "No se pudo consultar el historial reciente de Riot");

    try {
      JsonNode node = objectMapper.readTree(body);
      if (!node.isArray()) {
        throw malformed("Riot respondio con una lista de partidas mal formada", null);
      }
      List<String> matchIds = new ArrayList<>();
      node.forEach(item -> matchIds.add(item.asText()));
      return matchIds;
    } catch (RiotApiException e) {
      throw e;
    } catch (Exception e) {
      throw malformed("Riot respondio con una lista de partidas mal formada", e);
    }
  }

  @Override
  public RiotMatchDetails fetchMatchDetails(String matchId) {
    return observeRiot("match_detail", () -> fetchMatchDetailsInternal(matchId));
  }

  private RiotMatchDetails fetchMatchDetailsInternal(String matchId) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    String body =
        executeWithRetry(
            () ->
                riotClient(configuration)
                    .get()
                    .uri("/lol/match/v5/matches/{matchId}", matchId)
                    .retrieve()
                    .body(String.class),
            "No se pudo consultar el detalle de partida Riot");

    try {
      JsonNode root = objectMapper.readTree(body);
      JsonNode metadata = root.path("metadata");
      JsonNode info = root.path("info");
      String parsedMatchId = metadata.path("matchId").asText(matchId);
      JsonNode participants = info.path("participants");
      if (!participants.isArray()) {
        throw malformed("Riot respondio sin participantes de partida", null);
      }
      List<RiotMatchParticipant> parsedParticipants = new ArrayList<>();
      participants.forEach(participant -> parsedParticipants.add(parseParticipant(participant)));
      if (parsedParticipants.isEmpty()) {
        throw malformed("Riot respondio con una partida sin participantes", null);
      }
      return new RiotMatchDetails(
          parsedMatchId,
          info.path("gameMode").asText("Unknown"),
          info.path("queueId").isMissingNode() ? null : info.path("queueId").asInt(),
          info.path("gameDuration").asLong(0),
          Instant.ofEpochMilli(info.path("gameEndTimestamp").asLong(0)),
          info.path("platformId").asText(platformFromMatchId(parsedMatchId)),
          configuration.riotRegion().name(),
          parsedParticipants);
    } catch (RiotApiException e) {
      throw e;
    } catch (Exception e) {
      throw malformed("Riot respondio con un detalle de partida mal formado", e);
    }
  }

  @Override
  public List<RiotRankEntry> fetchRankEntries(RiotPlatform platform, String puuid) {
    return observeRiot("rank", () -> fetchRankEntriesInternal(platform, puuid));
  }

  private List<RiotRankEntry> fetchRankEntriesInternal(RiotPlatform platform, String puuid) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    String leagueBody =
        executeWithRetry(
            () ->
                platformClient(configuration, platform)
                    .get()
                    .uri("/lol/league/v4/entries/by-puuid/{encryptedPUUID}", puuid)
                    .retrieve()
                    .body(String.class),
            "No se pudo consultar el rank de Riot");

    try {
      JsonNode node = objectMapper.readTree(leagueBody);
      if (!node.isArray()) {
        throw malformed("Riot respondio con una lista de ranks mal formada", null);
      }
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
    } catch (RiotApiException e) {
      throw e;
    } catch (Exception e) {
      throw malformed("Riot respondio con una lista de ranks mal formada", e);
    }
  }

  @Override
  public String validateApiKey(RiotPlatform platform) {
    return observeRiot("validate_key", () -> validateApiKeyInternal(platform));
  }

  private String validateApiKeyInternal(RiotPlatform platform) {
    RuntimeAppConfiguration configuration = riotConfiguration();
    try {
      String body =
          executeWithRetry(
              () ->
                  platformClient(configuration, RiotPlatform.fromFormValue(platform))
                      .get()
                      .uri("/lol/status/v4/platform-data")
                      .retrieve()
                      .body(String.class),
              "No se pudo validar la Riot API key");
      JsonNode node = objectMapper.readTree(body);
      String name = node.path("name").asText(RiotPlatform.fromFormValue(platform).displayName());
      return "Riot OK en " + name;
    } catch (RiotApiException e) {
      throw e;
    } catch (Exception e) {
      throw malformed("Riot respondio, pero no se pudo interpretar la respuesta", e);
    }
  }

  private RiotMatchParticipant parseParticipant(JsonNode participant) {
    int creepScore =
        participant.path("totalMinionsKilled").asInt(0)
            + participant.path("neutralMinionsKilled").asInt(0);
    return new RiotMatchParticipant(
        participant.path("puuid").asText(),
        participant.path("championId").asInt(0),
        participant.path("championName").asText("Unknown"),
        firstText(participant, "teamPosition", "lane"),
        firstText(participant, "individualPosition", "role"),
        participant.path("kills").asInt(0),
        participant.path("deaths").asInt(0),
        participant.path("assists").asInt(0),
        creepScore,
        participant.path("goldEarned").asInt(0),
        participant.path("totalDamageDealtToChampions").asInt(0),
        participant.path("visionScore").asInt(0),
        participant.path("win").asBoolean(false));
  }

  private RuntimeAppConfiguration riotConfiguration() {
    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();
    if (configuration.riotApiKey() == null || configuration.riotApiKey().isBlank()) {
      throw new RiotApiException(RiotErrorCategory.NOT_CONFIGURED, "Riot API key no configurada");
    }
    return configuration;
  }

  private RestClient riotClient(RuntimeAppConfiguration configuration) {
    return restClientBuilder
        .clone()
        .baseUrl(configuration.riotRegion().baseUrl())
        .defaultHeader("X-Riot-Token", configuration.riotApiKey())
        .build();
  }

  private RestClient platformClient(RuntimeAppConfiguration configuration, RiotPlatform platform) {
    return restClientBuilder
        .clone()
        .baseUrl(RiotPlatform.fromFormValue(platform).baseUrl())
        .defaultHeader("X-Riot-Token", configuration.riotApiKey())
        .build();
  }

  private String executeWithRetry(Supplier<String> request, String fallbackMessage) {
    int attempts = Math.max(1, httpMaxAttempts);
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return request.get();
      } catch (RuntimeException e) {
        RiotApiException classified = classifyFailure(e, fallbackMessage);
        lastFailure = classified;
        if (attempt >= attempts || !isRetryable(classified)) {
          throw classified;
        }
        sleepBeforeRetry(attempt);
      }
    }
    throw lastFailure == null ? new RiotApiException(RiotErrorCategory.UNKNOWN, fallbackMessage) : lastFailure;
  }

  private boolean isRetryable(RiotApiException exception) {
    return exception.category() == RiotErrorCategory.TIMEOUT
        || exception.category() == RiotErrorCategory.UNAVAILABLE;
  }

  private RiotApiException classifyFailure(RuntimeException failure, String fallbackMessage) {
    if (failure instanceof RiotApiException riotApiException) {
      return riotApiException;
    }
    if (failure instanceof RestClientResponseException exception) {
      int status = exception.getStatusCode().value();
      if (status == 401 || status == 403) {
        return new RiotApiException(
            RiotErrorCategory.UNAUTHORIZED, "Riot API key invalida, caducada o sin permisos", failure);
      }
      if (status == 404) {
        return new RiotApiException(RiotErrorCategory.NOT_FOUND, "Cuenta o recurso Riot no encontrado", failure);
      }
      if (status == 429) {
        Duration retryAfter = retryAfter(exception);
        return new RiotApiException(
            RiotErrorCategory.RATE_LIMIT,
            "Riot rate limit alcanzado. Pausa sugerida: " + retryAfter.toSeconds() + "s",
            failure,
            retryAfter);
      }
      if (status == 400 || status == 405) {
        return new RiotApiException(
            RiotErrorCategory.WRONG_REGION_OR_PLATFORM,
            "Region o plataforma Riot incorrecta para esta operacion",
            failure);
      }
      if (status >= 500 && status <= 599) {
        return new RiotApiException(
            RiotErrorCategory.UNAVAILABLE, "Riot no esta disponible temporalmente", failure);
      }
    }
    if (failure instanceof ResourceAccessException) {
      return new RiotApiException(RiotErrorCategory.TIMEOUT, "Timeout o error de red llamando a Riot", failure);
    }
    return new RiotApiException(RiotErrorCategory.UNKNOWN, fallbackMessage, failure);
  }

  private Duration retryAfter(RestClientResponseException exception) {
    List<String> values = exception.getResponseHeaders() == null ? List.of() : exception.getResponseHeaders().get("Retry-After");
    if (values == null || values.isEmpty()) {
      return DEFAULT_RATE_LIMIT_PAUSE;
    }
    String value = values.get(0);
    try {
      long seconds = Long.parseLong(value.trim());
      return Duration.ofSeconds(Math.max(1, seconds));
    } catch (NumberFormatException ignored) {
      try {
        Instant retryAt =
            ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        Duration duration = Duration.between(clock.instant(), retryAt);
        return duration.isNegative() || duration.isZero() ? Duration.ofSeconds(1) : duration;
      } catch (RuntimeException ignoredAgain) {
        return DEFAULT_RATE_LIMIT_PAUSE;
      }
    }
  }

  private RiotApiException malformed(String message, Throwable cause) {
    return new RiotApiException(RiotErrorCategory.MALFORMED_RESPONSE, message, cause);
  }

  private <T> T observeRiot(String operation, Supplier<T> action) {
    long startedNanos = System.nanoTime();
    try {
      T result = action.get();
      recordRiot(operation, "OK", "NONE", startedNanos);
      return result;
    } catch (RiotApiException e) {
      recordRiot(operation, "ERROR", e.category().name(), startedNanos);
      throw e;
    } catch (RuntimeException e) {
      recordRiot(operation, "ERROR", "UNKNOWN", startedNanos);
      throw e;
    }
  }

  private void recordRiot(String operation, String status, String category, long startedNanos) {
    if (opsMetrics != null) {
      opsMetrics.recordRiotRequest(operation, status, category, System.nanoTime() - startedNanos);
    }
  }

  private void sleepBeforeRetry(int attempt) {
    try {
      Thread.sleep(httpRetryBackoff.multipliedBy(Math.max(1, attempt)).toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RiotApiException(RiotErrorCategory.UNKNOWN, "Retry Riot interrumpido", e);
    }
  }

  private String firstText(JsonNode node, String first, String second) {
    String value = node.path(first).asText("");
    if (value == null || value.isBlank()) {
      value = node.path(second).asText("");
    }
    return value == null ? "" : value;
  }

  private String platformFromMatchId(String matchId) {
    int separator = matchId == null ? -1 : matchId.indexOf('_');
    if (separator <= 0) {
      return "";
    }
    return matchId.substring(0, separator).toUpperCase(Locale.ROOT);
  }
}
