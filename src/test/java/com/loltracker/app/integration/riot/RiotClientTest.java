package com.loltracker.app.integration.riot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.loltracker.app.player.RiotPlatform;
import com.loltracker.app.ops.OpsMetrics;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.RiotRegion;
import com.loltracker.app.settings.RuntimeAppConfiguration;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class RiotClientTest {

  private static final String RIOT_KEY = "riot-test-key";
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-05-06T10:00:00Z"), ZoneOffset.UTC);

  private AppConfigurationService appConfigurationService;
  private RestClient.Builder restClientBuilder;
  private MockRestServiceServer server;
  private RiotClient riotClient;

  @BeforeEach
  void setUp() {
    appConfigurationService = mock(AppConfigurationService.class);
    when(appConfigurationService.getRuntimeConfiguration())
        .thenReturn(
            new RuntimeAppConfiguration(
                RIOT_KEY,
                RiotRegion.EUROPE,
                "",
                "",
                true,
                false,
                Duration.ofMinutes(5),
                10,
                3));
    restClientBuilder = RestClient.builder();
    server = MockRestServiceServer.bindTo(restClientBuilder).build();
    riotClient =
        new RiotClient(
            restClientBuilder,
            new ObjectMapper(),
            appConfigurationService,
            FIXED_CLOCK,
            mock(OpsMetrics.class));
    ReflectionTestUtils.setField(riotClient, "httpMaxAttempts", 1);
    ReflectionTestUtils.setField(riotClient, "httpRetryBackoff", Duration.ZERO);
  }

  @Test
  void fetchRecentMatchIdsClassifiesRateLimitAndReadsRetryAfter() {
    server
        .expect(once(), requestTo(matchIdsUrl(0, 10)))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Riot-Token", RIOT_KEY))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "7"));

    RiotApiException exception =
        assertThrows(RiotApiException.class, () -> riotClient.fetchRecentMatchIds("puuid-1", 0, 10));

    assertEquals(RiotErrorCategory.RATE_LIMIT, exception.category());
    assertEquals(Duration.ofSeconds(7), exception.retryAfter());
    server.verify();
  }

  @Test
  void fetchRecentMatchIdsClassifiesTimeouts() {
    server
        .expect(once(), requestTo(matchIdsUrl(0, 10)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withException(new SocketTimeoutException("read timed out")));

    RiotApiException exception =
        assertThrows(RiotApiException.class, () -> riotClient.fetchRecentMatchIds("puuid-1", 0, 10));

    assertEquals(RiotErrorCategory.TIMEOUT, exception.category());
    server.verify();
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 403})
  void fetchAccountClassifiesUnauthorizedResponses(int status) {
    server
        .expect(
            once(),
            requestTo(
                "https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Bazaga/ESP"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.valueOf(status)));

    RiotApiException exception =
        assertThrows(RiotApiException.class, () -> riotClient.fetchAccount("Bazaga", "ESP"));

    assertEquals(RiotErrorCategory.UNAUTHORIZED, exception.category());
    server.verify();
  }

  @Test
  void fetchAccountClassifiesNotFoundResponses() {
    server
        .expect(
            once(),
            requestTo(
                "https://europe.api.riotgames.com/riot/account/v1/accounts/by-riot-id/Missing/EUW"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    RiotApiException exception =
        assertThrows(RiotApiException.class, () -> riotClient.fetchAccount("Missing", "EUW"));

    assertEquals(RiotErrorCategory.NOT_FOUND, exception.category());
    server.verify();
  }

  @Test
  void fetchRankRetriesTransientServerErrors() {
    ReflectionTestUtils.setField(riotClient, "httpMaxAttempts", 2);
    server
        .expect(once(), requestTo("https://euw1.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/puuid-1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());
    server
        .expect(once(), requestTo("https://euw1.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/puuid-1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"id\":\"summoner-1\"}", MediaType.APPLICATION_JSON));
    server
        .expect(once(), requestTo("https://euw1.api.riotgames.com/lol/league/v4/entries/by-summoner/summoner-1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertEquals(0, riotClient.fetchRankEntries(RiotPlatform.EUW1, "puuid-1").size());
    server.verify();
  }

  @Test
  void fetchRecentMatchIdsClassifiesMalformedResponses() {
    server
        .expect(once(), requestTo(matchIdsUrl(0, 10)))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    RiotApiException exception =
        assertThrows(RiotApiException.class, () -> riotClient.fetchRecentMatchIds("puuid-1", 0, 10));

    assertEquals(RiotErrorCategory.MALFORMED_RESPONSE, exception.category());
    server.verify();
  }

  private String matchIdsUrl(int start, int count) {
    return "https://europe.api.riotgames.com/lol/match/v5/matches/by-puuid/puuid-1/ids?start="
        + start
        + "&count="
        + count;
  }
}
