package com.loltracker.playerservices.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.models.PlayerJson;
import com.loltracker.playerservices.domain.services.PlayerDataService;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@EnableScheduling
public class PlayerServiceUseCase {

  private final PlayerDataService playerDataService;
  private final ObjectMapper objectMapper;
  private List<PlayerJson> players;

  @PostConstruct
  public void loadPlayers() {
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("players.json");
      players = objectMapper.readValue(inputStream, new TypeReference<List<PlayerJson>>() {});
      System.out.println("Loaded players: " + players);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load players.json", e);
    }
  }

  public Mono<String> getSummonerData(String summonerName, String tagLine) {
    return playerDataService.getSummonerData(summonerName, tagLine);
  }

  @Scheduled(fixedRate = 1000 * 60)
  private void refreshPlayerData() {
    for (PlayerJson player : players) {
      getSummonerData(player.getSummonerName(), player.getTagLine())
          .subscribe(
              result -> {
                System.out.println("Summoner data refreshed for " + player.getSummonerName());
              },
              error -> {
                System.err.println(
                    "Error refreshing data for "
                        + player.getSummonerName()
                        + ": "
                        + error.getMessage());
              });
    }
  }
}
