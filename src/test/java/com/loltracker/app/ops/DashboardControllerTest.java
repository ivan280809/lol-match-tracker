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
import com.loltracker.app.player.PlayerForm;
import com.loltracker.app.player.PlayerService;
import com.loltracker.app.player.PlayerView;
import com.loltracker.app.tracking.PollSummary;
import com.loltracker.app.tracking.PollingService;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
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
      DashboardControllerTest.TestConfig.class
    })
@DirtiesContext
class DashboardControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private PlayerService playerService;
  @Autowired private TrackedMatchService trackedMatchService;
  @Autowired private PollRunService pollRunService;
  @Autowired private PollingService pollingService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    when(playerService.countPlayers()).thenReturn(1L);
    when(trackedMatchService.countMatches()).thenReturn(2L);
    when(playerService.getAllPlayers())
        .thenReturn(List.of(new PlayerView(7L, "Bazaga", "ESP", "puuid-1", true, null, null, "SUCCESS", null)));
    when(trackedMatchService.getRecentMatches()).thenReturn(List.of());
    when(pollRunService.getRecentRuns()).thenReturn(List.of());
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
    PollRunService pollRunService() {
      return mock(PollRunService.class);
    }

    @Bean
    @Primary
    PollingService pollingService() {
      return mock(PollingService.class);
    }
  }

  @Test
  void dashboardRendersCurrentData() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(model().attributeExists("dashboard", "players", "matches", "runs", "playerForm"))
        .andExpect(content().string(containsString("LOL Match Tracker")))
        .andExpect(content().string(containsString("Jugadores: 1")));
  }

  @Test
  void createPlayerWithInvalidFormReturnsDashboardWithErrors() throws Exception {
    mockMvc
        .perform(post("/players").param("gameName", " ").param("tagLine", ""))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(content().string(containsString("Nuevo jugador")));

    verify(playerService, never()).create(any(PlayerForm.class));
  }

  @Test
  void createPlayerWithDuplicateShowsFlashError() throws Exception {
    when(playerService.create(any(PlayerForm.class)))
        .thenThrow(new IllegalArgumentException("Player already exists"));

    mockMvc
        .perform(post("/players").param("gameName", "Bazaga").param("tagLine", "ESP").param("active", "true"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
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
}
