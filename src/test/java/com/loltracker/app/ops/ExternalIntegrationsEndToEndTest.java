package com.loltracker.app.ops;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotApiException;
import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.riot.RiotErrorCategory;
import com.loltracker.app.integration.riot.RiotRankEntry;
import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.notification.NotificationDeliveryStatus;
import com.loltracker.app.notification.NotificationOutboxEntity;
import com.loltracker.app.notification.NotificationOutboxRepository;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.app.player.RiotPlatform;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = {
      LolMatchTrackerApplication.class,
      ExternalIntegrationsEndToEndTest.TestConfig.class
    })
@DirtiesContext
class ExternalIntegrationsEndToEndTest {

  private static final String PLAYER_NAME = "Bazaga";
  private static final String PLAYER_TAG = "ESP";
  private static final String PUUID = "puuid-1";
  private static final String MATCH_ID = "EUW1_900";

  @Autowired private WebApplicationContext context;
  @Autowired private PlayerRepository playerRepository;
  @Autowired private TrackedMatchRepository trackedMatchRepository;
  @Autowired private NotificationOutboxRepository notificationOutboxRepository;
  @Autowired private ExternalCallLogRepository externalCallLogRepository;
  @Autowired private PollRunRepository pollRunRepository;
  @Autowired private RiotClient riotClient;
  @Autowired private TelegramNotifier telegramNotifier;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    reset(riotClient, telegramNotifier);
    externalCallLogRepository.deleteAll();
    notificationOutboxRepository.deleteAll();
    trackedMatchRepository.deleteAll();
    pollRunRepository.deleteAll();
    playerRepository.deleteAll();
    when(riotClient.isConfigured()).thenReturn(true);
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    RiotClient riotClient() {
      return mock(RiotClient.class);
    }

    @Bean
    @Primary
    TelegramNotifier telegramNotifier() {
      return mock(TelegramNotifier.class);
    }
  }

  @Test
  void fullServerFlowPersistsH2DataAndNeverCallsLiveIntegrations() throws Exception {
    when(riotClient.validateApiKey(RiotPlatform.EUW1)).thenReturn("Riot OK en Europe West");
    when(riotClient.fetchAccount(PLAYER_NAME, PLAYER_TAG)).thenReturn(account(PUUID, PLAYER_NAME, PLAYER_TAG));
    when(riotClient.fetchRecentMatchIds(PUUID)).thenReturn(List.of(MATCH_ID));
    when(riotClient.fetchMatchSummary(MATCH_ID, PUUID)).thenReturn(summary(MATCH_ID));
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, PUUID)).thenReturn(List.of(soloRank()));
    when(telegramNotifier.send(anyString())).thenReturn(new TelegramDeliveryReceipt(101));

    mockMvc
        .perform(post("/integrations/riot/validate").param("platform", "EUW1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=health"))
        .andExpect(flash().attribute("successMessage", "Riot OK en Europe West"));

    mockMvc
        .perform(
            post("/players/validate-account")
                .param("platform", "EUW1")
                .param("gameName", PLAYER_NAME)
                .param("tagLine", PLAYER_TAG)
                .param("active", "true")
                .param("returnTo", "/?tab=player"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=player"))
        .andExpect(flash().attribute("successMessage", "Cuenta Riot OK: Bazaga#ESP en EUW"));

    PlayerEntity player = createConfiguredPlayer(PLAYER_NAME, PLAYER_TAG, PUUID);

    mockMvc
        .perform(post("/api/operations/poll"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playersProcessed").value(1))
        .andExpect(jsonPath("$.newMatchesFound").value(1))
        .andExpect(jsonPath("$.notificationsSent").value(1))
        .andExpect(jsonPath("$.playerFailures").value(0))
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    assertEquals(1, playerRepository.count());
    assertEquals(1, trackedMatchRepository.count());
    assertEquals(1, notificationOutboxRepository.count());
    assertEquals(1, pollRunRepository.count());

    PlayerEntity syncedPlayer = playerRepository.findById(player.getId()).orElseThrow();
    assertEquals("SUCCESS", syncedPlayer.getLastSyncStatus());
    assertNull(syncedPlayer.getLastError());
    assertEquals("GOLD", syncedPlayer.getRankTier());
    assertEquals("II", syncedPlayer.getRankDivision());
    assertEquals(44, syncedPlayer.getRankLeaguePoints());

    TrackedMatchEntity trackedMatch = trackedMatchRepository.findAll().get(0);
    assertEquals(MATCH_ID, trackedMatch.getMatchId());
    assertTrue(trackedMatch.isNotificationSent());

    NotificationOutboxEntity outbox = notificationOutboxRepository.findAll().get(0);
    assertEquals(NotificationDeliveryStatus.SENT, outbox.getStatus());
    assertEquals(101, outbox.getTelegramMessageId());

    PollRunEntity run = pollRunRepository.findAll().get(0);
    assertEquals("SUCCESS", run.getStatus());
    assertNull(run.getErrorSummary());

    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("LOL Match Tracker")));
    mockMvc
        .perform(get("/players/{id}", player.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(PLAYER_NAME)))
        .andExpect(content().string(containsString("Lux")));
    mockMvc
        .perform(get("/audit"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Ultimos poll runs")));

    assertEquals(2, externalCallLogRepository.count());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void riotKeyValidationRecordsEveryRiotErrorCategory(RiotErrorCategory category) throws Exception {
    when(riotClient.validateApiKey(RiotPlatform.EUW1)).thenThrow(riotFailure(category));

    mockMvc
        .perform(post("/integrations/riot/validate").param("platform", "EUW1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=health"))
        .andExpect(flash().attribute("errorMessage", "Riot " + category.name()));

    assertExternalErrorLog("RIOT", "VALIDATE_KEY", category.name(), "Riot " + category.name());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void accountValidationRecordsEveryRiotErrorCategory(RiotErrorCategory category) throws Exception {
    when(riotClient.fetchAccount("Missing", "EUW")).thenThrow(riotFailure(category));

    mockMvc
        .perform(
            post("/players/validate-account")
                .param("platform", "EUW1")
                .param("gameName", "Missing")
                .param("tagLine", "EUW")
                .param("active", "true")
                .param("returnTo", "/?tab=player"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=player"))
        .andExpect(flash().attribute("errorMessage", "Riot " + category.name()));

    assertEquals(0, playerRepository.count());
    assertExternalErrorLog("RIOT", "VALIDATE_ACCOUNT", category.name(), "Riot " + category.name());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void playerCreationRejectsEveryRiotAccountErrorCategory(RiotErrorCategory category) throws Exception {
    when(riotClient.fetchAccount("Broken", "EUW")).thenThrow(riotFailure(category));

    mockMvc
        .perform(
            post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(playerJson("Broken", "EUW", true)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Riot " + category.name()));

    assertEquals(0, playerRepository.count());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void playerUpdateKeepsOriginalDataForEveryRiotAccountErrorCategory(RiotErrorCategory category)
      throws Exception {
    PlayerEntity player = createConfiguredPlayer(PLAYER_NAME, PLAYER_TAG, PUUID);
    when(riotClient.fetchAccount("Changed", "EUW")).thenThrow(riotFailure(category));

    mockMvc
        .perform(
            put("/api/players/{id}", player.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(playerJson("Changed", "EUW", true)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Riot " + category.name()));

    PlayerEntity persisted = playerRepository.findById(player.getId()).orElseThrow();
    assertEquals(PLAYER_NAME, persisted.getGameName());
    assertEquals(PLAYER_TAG, persisted.getTagLine());
    assertEquals(PUUID, persisted.getPuuid());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void pollingReportsPuuidResolutionFailuresForEveryRiotErrorCategory(RiotErrorCategory category)
      throws Exception {
    PlayerEntity player = createPlayerWithoutPuuid("NeedsPuuid", "EUW");
    when(riotClient.fetchAccount("NeedsPuuid", "EUW")).thenThrow(riotFailure(category));

    mockMvc
        .perform(post("/api/operations/poll"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playersProcessed").value(1))
        .andExpect(jsonPath("$.newMatchesFound").value(0))
        .andExpect(jsonPath("$.notificationsSent").value(0))
        .andExpect(jsonPath("$.playerFailures").value(1))
        .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"));

    assertFailedPlayerSync(player.getId(), category);
    assertPartialPollRunContains("NeedsPuuid#EUW: Riot " + category.name());
    assertEquals(0, trackedMatchRepository.count());
    verify(telegramNotifier, never()).send(anyString());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void pollingReportsMatchIdFailuresForEveryRiotErrorCategory(RiotErrorCategory category)
      throws Exception {
    PlayerEntity player = createConfiguredPlayer(PLAYER_NAME, PLAYER_TAG, PUUID);
    when(riotClient.fetchRecentMatchIds(PUUID)).thenThrow(riotFailure(category));

    mockMvc
        .perform(post("/api/operations/poll"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playersProcessed").value(1))
        .andExpect(jsonPath("$.newMatchesFound").value(0))
        .andExpect(jsonPath("$.notificationsSent").value(0))
        .andExpect(jsonPath("$.playerFailures").value(1))
        .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"));

    assertFailedPlayerSync(player.getId(), category);
    assertPartialPollRunContains(PLAYER_NAME + "#" + PLAYER_TAG + ": Riot " + category.name());
    assertEquals(0, trackedMatchRepository.count());
    verify(telegramNotifier, never()).send(anyString());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void pollingReportsMatchDetailFailuresForEveryRiotErrorCategory(RiotErrorCategory category)
      throws Exception {
    PlayerEntity player = createConfiguredPlayer(PLAYER_NAME, PLAYER_TAG, PUUID);
    when(riotClient.fetchRecentMatchIds(PUUID)).thenReturn(List.of(MATCH_ID));
    when(riotClient.fetchMatchSummary(MATCH_ID, PUUID)).thenThrow(riotFailure(category));

    mockMvc
        .perform(post("/api/operations/poll"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playersProcessed").value(1))
        .andExpect(jsonPath("$.newMatchesFound").value(0))
        .andExpect(jsonPath("$.notificationsSent").value(0))
        .andExpect(jsonPath("$.playerFailures").value(1))
        .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"));

    assertFailedPlayerSync(player.getId(), category);
    assertPartialPollRunContains(PLAYER_NAME + "#" + PLAYER_TAG + ": Riot " + category.name());
    assertEquals(0, trackedMatchRepository.count());
    assertEquals(0, notificationOutboxRepository.count());
    verify(telegramNotifier, never()).send(anyString());
  }

  @ParameterizedTest
  @EnumSource(RiotErrorCategory.class)
  void rankRefreshFailuresDoNotBreakPollingOrNotifications(RiotErrorCategory category)
      throws Exception {
    PlayerEntity player = createConfiguredPlayer(PLAYER_NAME, PLAYER_TAG, PUUID);
    when(riotClient.fetchRecentMatchIds(PUUID)).thenReturn(List.of(MATCH_ID));
    when(riotClient.fetchMatchSummary(MATCH_ID, PUUID)).thenReturn(summary(MATCH_ID));
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, PUUID)).thenThrow(riotFailure(category));
    when(telegramNotifier.send(anyString())).thenReturn(new TelegramDeliveryReceipt(202));

    mockMvc
        .perform(post("/api/operations/poll"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playersProcessed").value(1))
        .andExpect(jsonPath("$.newMatchesFound").value(1))
        .andExpect(jsonPath("$.notificationsSent").value(1))
        .andExpect(jsonPath("$.playerFailures").value(0))
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    PlayerEntity syncedPlayer = playerRepository.findById(player.getId()).orElseThrow();
    assertEquals("SUCCESS", syncedPlayer.getLastSyncStatus());
    assertNull(syncedPlayer.getLastError());
    assertNull(syncedPlayer.getRankTier());

    NotificationOutboxEntity outbox = notificationOutboxRepository.findAll().get(0);
    assertEquals(NotificationDeliveryStatus.SENT, outbox.getStatus());
    assertEquals(202, outbox.getTelegramMessageId());
  }

  @ParameterizedTest
  @MethodSource("telegramFailures")
  void telegramTestActionRecordsEveryFailureType(RuntimeException exception, String expectedCategory)
      throws Exception {
    when(telegramNotifier.send(anyString())).thenThrow(exception);

    mockMvc
        .perform(post("/integrations/telegram/test"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=health"))
        .andExpect(flash().attribute("errorMessage", exception.getMessage()));

    assertExternalErrorLog("TELEGRAM", "TEST_SEND", expectedCategory, exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("telegramFailures")
  void telegramNotificationFailuresKeepOutboxRetryable(RuntimeException exception, String ignored)
      throws Exception {
    PlayerEntity player = createConfiguredPlayer(PLAYER_NAME, PLAYER_TAG, PUUID);
    when(riotClient.fetchRecentMatchIds(PUUID)).thenReturn(List.of(MATCH_ID));
    when(riotClient.fetchMatchSummary(MATCH_ID, PUUID)).thenReturn(summary(MATCH_ID));
    when(riotClient.fetchRankEntries(RiotPlatform.EUW1, PUUID)).thenReturn(List.of(soloRank()));
    when(telegramNotifier.send(anyString())).thenThrow(exception);

    mockMvc
        .perform(post("/api/operations/poll"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playersProcessed").value(1))
        .andExpect(jsonPath("$.newMatchesFound").value(1))
        .andExpect(jsonPath("$.notificationsSent").value(0))
        .andExpect(jsonPath("$.playerFailures").value(0))
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    PlayerEntity syncedPlayer = playerRepository.findById(player.getId()).orElseThrow();
    assertEquals("SUCCESS", syncedPlayer.getLastSyncStatus());
    assertNull(syncedPlayer.getLastError());

    TrackedMatchEntity trackedMatch = trackedMatchRepository.findAll().get(0);
    assertFalse(trackedMatch.isNotificationSent());

    NotificationOutboxEntity outbox = notificationOutboxRepository.findAll().get(0);
    assertEquals(NotificationDeliveryStatus.FAILED, outbox.getStatus());
    assertEquals(1, outbox.getAttemptCount());
    assertEquals(exception.getMessage(), outbox.getLastError());
    assertNotNull(outbox.getNextAttemptAt());
  }

  private PlayerEntity createConfiguredPlayer(String gameName, String tagLine, String puuid)
      throws Exception {
    when(riotClient.fetchAccount(gameName, tagLine)).thenReturn(account(puuid, gameName, tagLine));

    mockMvc
        .perform(
            post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(gameName, tagLine, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.platform").value("EUW1"))
        .andExpect(jsonPath("$.gameName").value(gameName))
        .andExpect(jsonPath("$.tagLine").value(tagLine))
        .andExpect(jsonPath("$.puuid").value(puuid));

    clearInvocations(riotClient, telegramNotifier);
    return playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine).orElseThrow();
  }

  private PlayerEntity createPlayerWithoutPuuid(String gameName, String tagLine) throws Exception {
    when(riotClient.isConfigured()).thenReturn(false);
    mockMvc
        .perform(
            post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content(playerJson(gameName, tagLine, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.puuid").doesNotExist());
    when(riotClient.isConfigured()).thenReturn(true);
    clearInvocations(riotClient, telegramNotifier);
    return playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(gameName, tagLine).orElseThrow();
  }

  private void assertFailedPlayerSync(Long playerId, RiotErrorCategory category) {
    PlayerEntity player = playerRepository.findById(playerId).orElseThrow();
    assertEquals("ERROR", player.getLastSyncStatus());
    assertEquals("Riot " + category.name(), player.getLastError());
    assertNotNull(player.getLastPolledAt());
  }

  private void assertPartialPollRunContains(String expectedErrorFragment) {
    PollRunEntity run = pollRunRepository.findAll().get(0);
    assertEquals("PARTIAL_SUCCESS", run.getStatus());
    assertTrue(run.getErrorSummary().contains(expectedErrorFragment));
  }

  private void assertExternalErrorLog(
      String integration, String operation, String category, String summary) {
    assertEquals(1, externalCallLogRepository.count());
    ExternalCallLogEntity log = externalCallLogRepository.findAll().get(0);
    assertEquals(integration, log.getIntegration());
    assertEquals(operation, log.getOperation());
    assertEquals("ERROR", log.getStatus());
    assertEquals(category, log.getCategory());
    assertEquals(summary, log.getSummary());
    assertNotNull(log.getOccurredAt());
  }

  private RiotApiException riotFailure(RiotErrorCategory category) {
    return new RiotApiException(category, "Riot " + category.name());
  }

  private RiotAccount account(String puuid, String gameName, String tagLine) {
    return new RiotAccount(puuid, gameName, tagLine);
  }

  private MatchSummary summary(String matchId) {
    return new MatchSummary(
        matchId, "Lux", true, "CLASSIC", 1800, Instant.parse("2026-04-03T18:00:00Z"));
  }

  private RiotRankEntry soloRank() {
    return new RiotRankEntry("RANKED_SOLO_5x5", "GOLD", "II", 44, 20, 10);
  }

  private String playerJson(String gameName, String tagLine, boolean active) {
    return """
        {"platform":"EUW1","gameName":"%s","tagLine":"%s","active":%s}
        """
        .formatted(gameName, tagLine, active);
  }

  private static Stream<Arguments> telegramFailures() {
    return Stream.of(
        Arguments.of(new IllegalStateException("Telegram is not configured"), "IllegalStateException"),
        Arguments.of(new IllegalArgumentException("Telegram unauthorized"), "IllegalArgumentException"),
        Arguments.of(new RuntimeException("Telegram rate limit"), "RuntimeException"),
        Arguments.of(new RuntimeException("Telegram timeout"), "RuntimeException"),
        Arguments.of(new RuntimeException("Telegram unavailable"), "RuntimeException"),
        Arguments.of(new RuntimeException("Telegram unknown"), "RuntimeException"));
  }
}
