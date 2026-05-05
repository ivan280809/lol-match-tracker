package com.loltracker.app.ops;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.loltracker.app.match.TrackedMatchService;
import com.loltracker.app.match.PlayerRecentStatsView;
import com.loltracker.app.notification.NotificationService;
import com.loltracker.app.player.PlayerForm;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.player.PlayerView;
import com.loltracker.app.settings.AppConfigurationForm;
import com.loltracker.app.settings.AppConfigurationService;
import com.loltracker.app.settings.AppConfigurationView;
import com.loltracker.app.settings.RiotRegion;
import com.loltracker.app.tracking.PollSummary;
import com.loltracker.app.tracking.PollingService;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
      DashboardControllerTest.TestConfig.class
    })
@DirtiesContext
class DashboardControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private PlayerService playerService;
  @Autowired private TrackedMatchService trackedMatchService;
  @Autowired private NotificationService notificationService;
  @Autowired private PollRunService pollRunService;
  @Autowired private PollingService pollingService;
  @Autowired private AppConfigurationService appConfigurationService;
  @Autowired private RosterQueryService rosterQueryService;
  @Autowired private PlayerPageService playerPageService;
  @Autowired private OpsHealthService opsHealthService;
  @Autowired private IntegrationOperationsService integrationOperationsService;
  @Autowired private AuditService auditService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    when(playerService.countPlayers()).thenReturn(1L);
    when(trackedMatchService.countMatches()).thenReturn(2L);
    when(notificationService.countPendingNotifications()).thenReturn(3L);
    when(notificationService.countFailedNotifications()).thenReturn(1L);
    PlayerView player = new PlayerView(7L, "Bazaga", "ESP", "puuid-1", true, null, null, "SUCCESS", null);
    when(playerService.getAllPlayers()).thenReturn(List.of(player));
    when(rosterQueryService.getRoster(any(DashboardFilter.class))).thenReturn(List.of(player));
    when(trackedMatchService.getRecentMatches()).thenReturn(List.of());
    when(pollRunService.getRecentRuns()).thenReturn(List.of());
    when(appConfigurationService.getView())
        .thenReturn(new AppConfigurationView(true, RiotRegion.EUROPE, true, true, true));
    when(appConfigurationService.getForm())
        .thenReturn(new AppConfigurationForm("", RiotRegion.EUROPE.name(), "", ""));
    when(opsHealthService.currentHealth())
        .thenReturn(new IntegrationHealthView(true, "DB OK", true, true, null, null, 3L, 1L));
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    PlayerService playerService() {
      return mock(PlayerService.class);
    }

    @Bean
    @Primary
    TrackedMatchService trackedMatchService() {
      return mock(TrackedMatchService.class);
    }

    @Bean
    @Primary
    NotificationService notificationService() {
      return mock(NotificationService.class);
    }

    @Bean
    @Primary
    PollRunService pollRunService() {
      return mock(PollRunService.class);
    }

    @Bean
    @Primary
    PollingService pollingService() {
      return mock(PollingService.class);
    }

    @Bean
    @Primary
    AppConfigurationService appConfigurationService() {
      return mock(AppConfigurationService.class);
    }

    @Bean
    @Primary
    RosterQueryService rosterQueryService() {
      return mock(RosterQueryService.class);
    }

    @Bean
    @Primary
    PlayerPageService playerPageService() {
      return mock(PlayerPageService.class);
    }

    @Bean
    @Primary
    OpsHealthService opsHealthService() {
      return mock(OpsHealthService.class);
    }

    @Bean
    @Primary
    IntegrationOperationsService integrationOperationsService() {
      return mock(IntegrationOperationsService.class);
    }

    @Bean
    @Primary
    AuditService auditService() {
      return mock(AuditService.class);
    }
  }

  @Test
  void dashboardRendersCurrentData() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(
            model()
                .attributeExists(
                    "dashboard",
                    "players",
                    "matches",
                    "runs",
                    "playerForm",
                    "configuration",
                    "configurationForm",
                    "health",
                    "filters",
                    "riotRegions",
                    "riotPlatforms"))
        .andExpect(content().string(containsString("LOL Match Tracker")))
        .andExpect(content().string(containsString("Roster, sincronizacion y salud operativa.")))
        .andExpect(content().string(containsString("Configuracion")))
        .andExpect(content().string(containsString("Filtros y ordenacion")))
        .andExpect(content().string(containsString("Orden: Nombre")))
        .andExpect(content().string(containsString("data-autosubmit")))
        .andExpect(content().string(containsString("Integraciones")))
        .andExpect(content().string(containsString("Editar")))
        .andExpect(content().string(containsString("Servidor")))
        .andExpect(content().string(containsString(">1<")));
  }

  @Test
  void dashboardReflectsSelectedSortAndTabState() throws Exception {
    mockMvc
        .perform(get("/").param("sort", "rank").param("tab", "health"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("activeTab", "health"))
        .andExpect(content().string(containsString("Orden: Rank")))
        .andExpect(content().string(containsString("id=\"tab-health\" name=\"side-tab\" checked=\"checked\"")));
  }

  @Test
  void dashboardPassesFiltersToRosterQuery() throws Exception {
    org.mockito.Mockito.clearInvocations(rosterQueryService);

    mockMvc
        .perform(get("/").param("status", "archived").param("withError", "true").param("sort", "rank"))
        .andExpect(status().isOk());

    verify(rosterQueryService)
        .getRoster(
            org.mockito.ArgumentMatchers.argThat(
                filter -> filter.status().equals("archived") && filter.withError() && filter.sort().equals("rank")));
  }

  @Test
  void dashboardNormalizesUnknownFilterParams() throws Exception {
    org.mockito.Mockito.clearInvocations(rosterQueryService);

    mockMvc
        .perform(get("/").param("status", "surprise").param("sort", "sideways"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Orden: Nombre")));

    verify(rosterQueryService)
        .getRoster(
            org.mockito.ArgumentMatchers.argThat(
                filter -> filter.status().equals("active") && filter.sort().equals("name")));
  }

  @Test
  void createPlayerWithInvalidFormReturnsDashboardWithErrors() throws Exception {
    mockMvc
        .perform(post("/players").param("platform", "EUW1").param("gameName", " ").param("tagLine", ""))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(content().string(containsString("Nuevo jugador")));

    verify(playerService, never()).create(any(PlayerForm.class));
  }

  @Test
  void getPlayersRedirectsToDashboard() throws Exception {
    mockMvc
        .perform(get("/players"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void playerDetailRendersOperationalData() throws Exception {
    PlayerView player = new PlayerView(7L, "Bazaga", "ESP", "puuid-1", true, null, null, "SUCCESS", null);
    when(playerPageService.getDetail(7L))
        .thenReturn(new PlayerDetailView(player, List.of(), PlayerRecentStatsView.empty(), List.of()));

    mockMvc
        .perform(get("/players/7"))
        .andExpect(status().isOk())
        .andExpect(view().name("player-detail"))
        .andExpect(content().string(containsString("Detalle de jugador")))
        .andExpect(content().string(containsString("Bazaga#ESP")));
  }

  @Test
  void editPlayerRendersForm() throws Exception {
    when(playerService.getPlayer(7L))
        .thenReturn(new PlayerView(7L, "Bazaga", "ESP", "puuid-1", true, null, null, "SUCCESS", null));

    mockMvc
        .perform(get("/players/7/edit"))
        .andExpect(status().isOk())
        .andExpect(view().name("player-edit"))
        .andExpect(content().string(containsString("Editar jugador")))
        .andExpect(content().string(containsString("Validar cuenta Riot")));
  }

  @Test
  void editPlayerKeepsValidatedDraftFromFlash() throws Exception {
    when(playerService.getPlayer(7L))
        .thenReturn(new PlayerView(7L, "Bazaga", "ESP", "puuid-1", true, null, null, "SUCCESS", null));

    mockMvc
        .perform(
            get("/players/7/edit")
                .flashAttr(
                    "playerForm",
                    new PlayerForm(com.loltracker.app.player.RiotPlatform.NA1, "DraftName", "NA", false)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("DraftName")));
  }

  @Test
  void updatePlayerRedirectsToDetail() throws Exception {
    mockMvc
        .perform(
            post("/players/7")
                .param("platform", "EUW1")
                .param("gameName", "Bazaga")
                .param("tagLine", "ESP")
                .param("active", "true"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/players/7"))
        .andExpect(flash().attribute("successMessage", "Jugador actualizado"));

    verify(playerService).update(7L, new PlayerForm(com.loltracker.app.player.RiotPlatform.EUW1, "Bazaga", "ESP", true));
  }

  @Test
  void archiveAndRestoreUseLogicalDeleteFlow() throws Exception {
    mockMvc
        .perform(post("/players/7/archive").param("returnTo", "/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andExpect(flash().attribute("successMessage", "Jugador archivado"));
    verify(playerService).archive(7L);

    mockMvc
        .perform(post("/players/7/restore").param("returnTo", "/players/7"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/players/7"))
        .andExpect(flash().attribute("successMessage", "Jugador restaurado"));
    verify(playerService).restore(7L);
  }

  @Test
  void createPlayerWithDuplicateShowsFlashError() throws Exception {
    when(playerService.create(any(PlayerForm.class)))
        .thenThrow(new IllegalArgumentException("Player already exists"));

    mockMvc
        .perform(
            post("/players")
                .param("platform", "EUW1")
                .param("gameName", "Bazaga")
                .param("tagLine", "ESP")
                .param("active", "true")
                .param("returnTo", "/?tab=player"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=player"))
        .andExpect(flash().attribute("errorMessage", "Player already exists"));
  }

  @Test
  void runPollingWithFailuresShowsErrorFlash() throws Exception {
    when(pollingService.runPoll()).thenReturn(new PollSummary(2, 1, 1, 1, "PARTIAL_SUCCESS"));

    mockMvc
        .perform(post("/polling/run"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andExpect(
            flash()
                .attribute(
                    "errorMessage",
                    "Polling ejecutado. Estado: PARTIAL_SUCCESS, jugadores: 2, nuevas partidas: 1, avisos: 1"));
  }

  @Test
  void dashboardShowsActivePollingStateAndDisablesManualButton() throws Exception {
    when(pollRunService.getActiveRun())
        .thenReturn(
            Optional.of(
                new PollRunView(
                    99L,
                    Instant.parse("2026-05-06T10:00:00Z"),
                    null,
                    "RUNNING",
                    1,
                    0,
                    0,
                    null,
                    "Bazaga#ESP",
                    "Consultando historial Riot",
                    null,
                    null)));

    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("En curso: Bazaga#ESP - Consultando historial Riot")))
        .andExpect(content().string(containsString("disabled=\"disabled\"")));
  }

  @Test
  void runPollingAlreadyActiveShowsClearErrorFlash() throws Exception {
    when(pollingService.runPoll()).thenReturn(new PollSummary(0, 0, 0, 0, "SKIPPED"));

    mockMvc
        .perform(post("/polling/run"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andExpect(
            flash()
                .attribute(
                    "errorMessage",
                    "Polling ejecutado. Estado: SKIPPED, jugadores: 0, nuevas partidas: 0, avisos: 0"));
  }

  @Test
  void dashboardShowsRateLimitPauseAndDisablesManualButton() throws Exception {
    when(pollRunService.getActiveRateLimitPause())
        .thenReturn(
            Optional.of(
                new PollRunView(
                    100L,
                    Instant.parse("2026-05-06T10:00:00Z"),
                    Instant.parse("2026-05-06T10:00:01Z"),
                    "RATE_LIMITED",
                    1,
                    0,
                    0,
                    "Riot rate limit activo",
                    null,
                    null,
                    Instant.parse("2026-05-06T10:02:00Z"),
                    "Riot rate limit activo. Reintentar tras 120s")));

    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Riot pausado hasta 2026-05-06T10:02:00Z")))
        .andExpect(content().string(containsString("disabled=\"disabled\"")));
  }

  @Test
  void runPollingPreservesCurrentDashboardQuery() throws Exception {
    when(pollingService.runPoll()).thenReturn(new PollSummary(1, 0, 0, 0, "SUCCESS"));

    mockMvc
        .perform(post("/polling/run").param("returnTo", "/?sort=rank&tab=health"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?sort=rank&tab=health"));
  }

  @Test
  void updateConfigurationShowsSuccessFlash() throws Exception {
    mockMvc
        .perform(
            post("/configuration")
                .param("riotRegion", "AMERICAS")
                .param("riotApiKey", "riot-key")
                .param("telegramBotToken", "telegram-token")
                .param("telegramChatId", "chat-id")
                .param("pollingEnabled", "true")
                .param("pollingManualOnly", "true")
                .param("pollingFixedDelay", "PT10M")
                .param("pollingMatchWindowSize", "20")
                .param("pollingPaginationLimit", "4"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=config"))
        .andExpect(flash().attribute("successMessage", "Configuracion guardada"));

    verify(appConfigurationService)
        .update(
            new AppConfigurationForm(
                "riot-key", "AMERICAS", "telegram-token", "chat-id", true, true, "PT10M", 20, 4));
  }

  @Test
  void validateRiotKeyShowsSuccessFlash() throws Exception {
    when(integrationOperationsService.validateRiotKey(com.loltracker.app.player.RiotPlatform.EUW1))
        .thenReturn(new IntegrationActionResult(true, "OK", "Riot OK en Europe West"));

    mockMvc
        .perform(post("/integrations/riot/validate").param("platform", "EUW1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=health"))
        .andExpect(flash().attribute("successMessage", "Riot OK en Europe West"));
  }

  @Test
  void testTelegramShowsFriendlyErrorFlash() throws Exception {
    when(integrationOperationsService.testTelegram())
        .thenReturn(new IntegrationActionResult(false, "UNAUTHORIZED", "Telegram is not configured"));

    mockMvc
        .perform(post("/integrations/telegram/test"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?tab=health"))
        .andExpect(flash().attribute("errorMessage", "Telegram is not configured"));
  }

  @Test
  void validateAccountShowsFlashWithoutSaving() throws Exception {
    when(integrationOperationsService.validateAccount("Bazaga", "ESP", com.loltracker.app.player.RiotPlatform.EUW1))
        .thenReturn(new IntegrationActionResult(true, "OK", "Cuenta Riot OK: Bazaga#ESP en EUW"));

    mockMvc
        .perform(
            post("/players/validate-account")
                .param("platform", "EUW1")
                .param("gameName", "Bazaga")
                .param("tagLine", "ESP")
                .param("active", "true")
                .param("returnTo", "/players/7/edit"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/players/7/edit"))
        .andExpect(flash().attribute("successMessage", "Cuenta Riot OK: Bazaga#ESP en EUW"))
        .andExpect(flash().attributeExists("playerForm"));

    verify(playerService, never()).create(any(PlayerForm.class));
  }

  @Test
  void auditRendersOperationalAudit() throws Exception {
    when(auditService.getAudit()).thenReturn(new AuditView(List.of(), List.of(), List.of(), List.of()));

    mockMvc
        .perform(get("/audit"))
        .andExpect(status().isOk())
        .andExpect(view().name("audit"))
        .andExpect(content().string(containsString("Auditoria")))
        .andExpect(content().string(containsString("Respuestas externas resumidas")));
  }

  @Test
  void unexpectedDashboardExceptionsRedirectWithGenericToast() throws Exception {
    org.mockito.Mockito.doThrow(new IllegalStateException("database exploded"))
        .when(playerService)
        .setActive(7L, false);

    mockMvc
        .perform(post("/players/7/toggle").param("active", "false"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andExpect(flash().attribute("errorMessage", "Un error ha ocurrido"));
  }
}
