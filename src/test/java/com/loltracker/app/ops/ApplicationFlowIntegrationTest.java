package com.loltracker.app.ops;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.notification.NotificationOutboxRepository;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = {
      LolMatchTrackerApplication.class,
      ApplicationFlowIntegrationTest.TestConfig.class
    })
@DirtiesContext
class ApplicationFlowIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private PlayerRepository playerRepository;
  @Autowired private TrackedMatchRepository trackedMatchRepository;
  @Autowired private NotificationOutboxRepository notificationOutboxRepository;
  @Autowired private ExternalCallLogRepository externalCallLogRepository;
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
  void uiAndPollingFlowWorkWithoutLiveIntegrations() throws Exception {
    when(riotClient.fetchAccount("Bazaga", "ESP")).thenReturn(new RiotAccount("puuid-1", "Bazaga", "ESP"));

    mockMvc
        .perform(
            post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameName\":\"Bazaga\",\"tagLine\":\"ESP\",\"active\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.platform").value("EUW1"))
        .andExpect(jsonPath("$.gameName").value("Bazaga"))
        .andExpect(jsonPath("$.puuid").value("puuid-1"));

    when(riotClient.fetchRecentMatchIds("puuid-1")).thenReturn(java.util.List.of("EUW1_900"));
    when(riotClient.fetchMatchSummary("EUW1_900", "puuid-1"))
        .thenReturn(
            new MatchSummary(
                "EUW1_900", "Lux", true, "CLASSIC", 1800, Instant.parse("2026-04-03T18:00:00Z")));

    mockMvc.perform(post("/api/operations/poll")).andExpect(status().isOk()).andExpect(jsonPath("$.newMatchesFound").value(1));

    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("LOL Match Tracker")));

    verify(telegramNotifier).send(anyString());
    org.junit.jupiter.api.Assertions.assertEquals(1, playerRepository.count());
    org.junit.jupiter.api.Assertions.assertEquals(1, trackedMatchRepository.count());
  }

  @Test
  void archivedPlayersKeepHistoryButDoNotPoll() throws Exception {
    when(riotClient.fetchAccount("Bazaga", "ESP")).thenReturn(new RiotAccount("puuid-1", "Bazaga", "ESP"));

    mockMvc
        .perform(
            post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameName\":\"Bazaga\",\"tagLine\":\"ESP\",\"active\":true}"))
        .andExpect(status().isOk());

    Long playerId =
        playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("Bazaga", "ESP").orElseThrow().getId();

    mockMvc.perform(post("/players/{id}/archive", playerId).param("returnTo", "/")).andExpect(status().is3xxRedirection());
    clearInvocations(riotClient);

    mockMvc.perform(post("/api/operations/poll")).andExpect(status().isOk()).andExpect(jsonPath("$.playersProcessed").value(0));

    verify(riotClient, never()).fetchRecentMatchIds(anyString());
    org.junit.jupiter.api.Assertions.assertEquals(1, playerRepository.count());
    org.junit.jupiter.api.Assertions.assertTrue(playerRepository.findById(playerId).orElseThrow().getArchivedAt() != null);
  }

  @Test
  void telegramTestActionRecordsExternalLogWithoutRealTelegram() throws Exception {
    when(telegramNotifier.send(anyString())).thenReturn(new TelegramDeliveryReceipt(44));

    mockMvc
        .perform(post("/integrations/telegram/test"))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("successMessage", "Telegram OK (mensaje 44)"));

    ExternalCallLogEntity log = externalCallLogRepository.findAll().get(0);
    org.junit.jupiter.api.Assertions.assertEquals("TELEGRAM", log.getIntegration());
    org.junit.jupiter.api.Assertions.assertEquals("OK", log.getStatus());
    org.junit.jupiter.api.Assertions.assertEquals("Telegram OK (mensaje 44)", log.getSummary());
  }

  @Test
  void duplicateUpdateReturnsBadRequestInsteadOfServerError() throws Exception {
    when(riotClient.fetchAccount("Bazaga", "ESP")).thenReturn(new RiotAccount("puuid-1", "Bazaga", "ESP"));
    when(riotClient.fetchAccount("LuxMain", "EUW")).thenReturn(new RiotAccount("puuid-2", "LuxMain", "EUW"));

    String firstResponse =
        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"gameName\":\"Bazaga\",\"tagLine\":\"ESP\",\"active\":true}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    mockMvc
        .perform(
            post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameName\":\"LuxMain\",\"tagLine\":\"EUW\",\"active\":true}"))
        .andExpect(status().isOk());

    Long secondId =
        playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase("LuxMain", "EUW").orElseThrow().getId();

    mockMvc
        .perform(
            put("/api/players/{id}", secondId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameName\":\"Bazaga\",\"tagLine\":\"ESP\",\"active\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Player already exists"));

    org.junit.jupiter.api.Assertions.assertEquals(2, playerRepository.count());
  }
}
