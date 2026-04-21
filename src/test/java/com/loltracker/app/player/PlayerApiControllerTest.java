package com.loltracker.app.player;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
      PlayerApiControllerTest.TestConfig.class
    })
@DirtiesContext
class PlayerApiControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private PlayerService playerService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    PlayerService playerService() {
      return mock(PlayerService.class);
    }
  }

  @Test
  void getPlayersReturnsServicePayload() throws Exception {
    when(playerService.getAllPlayers())
        .thenReturn(List.of(new PlayerView(7L, "Bazaga", "ESP", "puuid-1", true, null, null, null, null)));

    mockMvc
        .perform(get("/api/players"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(7))
        .andExpect(jsonPath("$[0].gameName").value("Bazaga"))
        .andExpect(jsonPath("$[0].puuid").value("puuid-1"));
  }

  @Test
  void getPlayerReturnsProblemDetailWhenNotFound() throws Exception {
    when(playerService.getPlayer(99L)).thenThrow(new EntityNotFoundException("Player not found"));

    mockMvc
        .perform(get("/api/players/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Player not found"));
  }

  @Test
  void createPlayerReturnsValidationErrors() throws Exception {
    mockMvc
        .perform(
            post("/api/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"gameName\":\" \",\"tagLine\":\"\",\"active\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Validation failed"))
        .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("gameName", "tagLine")));
  }

  @Test
  void setActivationDelegatesToService() throws Exception {
    mockMvc
        .perform(post("/api/players/7/activation").param("active", "false"))
        .andExpect(status().isOk());

    verify(playerService).setActive(7L, false);
  }
}
