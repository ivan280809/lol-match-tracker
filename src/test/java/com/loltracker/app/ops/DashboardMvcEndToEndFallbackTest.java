package com.loltracker.app.ops;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loltracker.app.integration.riot.RiotAccount;
import com.loltracker.app.integration.riot.RiotClient;
import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotifier;
import com.loltracker.app.match.MatchRepository;
import com.loltracker.app.match.MatchSummary;
import com.loltracker.app.match.PlayerMatchRepository;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.notification.NotificationOutboxRepository;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    classes = {
      LolMatchTrackerApplication.class,
      DashboardMvcEndToEndFallbackTest.TestConfig.class
    })
@DirtiesContext
class DashboardMvcEndToEndFallbackTest {

  private static final String PLAYER_NAME = "Bazaga";
  private static final String PLAYER_TAG = "ESP";
  private static final String PUUID = "puuid-1";
  private static final String MATCH_ID = "EUW1_900";

  @Autowired private WebApplicationContext context;
  @Autowired private PlayerRepository playerRepository;
  @Autowired private TrackedMatchRepository trackedMatchRepository;
  @Autowired private MatchRepository matchRepository;
  @Autowired private PlayerMatchRepository playerMatchRepository;
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
    playerMatchRepository.deleteAll();
    matchRepository.deleteAll();
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
  void serverRenderedUserFlowWorksThroughMvcWithoutLiveIntegrations() throws Exception {
    when(riotClient.fetchAccount(PLAYER_NAME, PLAYER_TAG)).thenReturn(new RiotAccount(PUUID, PLAYER_NAME, PLAYER_TAG));
    when(riotClient.fetchRecentMatchIds(PUUID)).thenReturn(List.of(MATCH_ID));
    when(riotClient.fetchMatchSummary(MATCH_ID, PUUID))
        .thenReturn(
            new MatchSummary(
                MATCH_ID,
                99,
                "Lux",
                true,
                "CLASSIC",
                420,
                "MIDDLE",
                "SOLO",
                8,
                2,
                11,
                210,
                13200,
                26000,
                28,
                1800,
                Instant.parse("2030-04-03T18:00:00Z"),
                "EUW1",
                "EUROPE"));
    when(telegramNotifier.send(anyString())).thenReturn(new TelegramDeliveryReceipt(101));

    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("LOL Match Tracker")))
        .andExpect(content().string(containsString("Nuevo jugador")));

    mockMvc
        .perform(
            post("/players")
                .param("platform", "EUW1")
                .param("gameName", PLAYER_NAME)
                .param("tagLine", PLAYER_TAG)
                .param("active", "true")
                .param("returnTo", "/?tab=player"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=player"))
        .andExpect(flash().attribute("successMessage", "Jugador creado"));

    PlayerEntity player =
        playerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase(PLAYER_NAME, PLAYER_TAG).orElseThrow();
    clearInvocations(riotClient, telegramNotifier);

    when(riotClient.fetchRecentMatchIds(PUUID)).thenReturn(List.of(MATCH_ID));
    when(riotClient.fetchMatchSummary(MATCH_ID, PUUID))
        .thenReturn(new MatchSummary(MATCH_ID, "Lux", true, "CLASSIC", 1800, Instant.parse("2030-04-03T18:00:00Z")));
    when(telegramNotifier.send(anyString())).thenReturn(new TelegramDeliveryReceipt(101));

    mockMvc
        .perform(post("/polling/run").param("returnTo", "/?tab=health"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=health"))
        .andExpect(
            flash()
                .attribute(
                    "successMessage",
                    "Polling ejecutado. Estado: SUCCESS, jugadores: 1, nuevas partidas: 1, avisos: 1"));

    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(PLAYER_NAME + "#" + PLAYER_TAG)))
        .andExpect(content().string(containsString("Lux")));
    mockMvc
        .perform(get("/players/{id}", player.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Detalle de jugador")))
        .andExpect(content().string(containsString("Lux")));
    mockMvc
        .perform(get("/audit"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Auditoria")))
        .andExpect(content().string(containsString("Ultimos poll runs")));

    verify(telegramNotifier).send(anyString());
  }
}
