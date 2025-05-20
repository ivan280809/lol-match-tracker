package com.loltracker.playerservices.application.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.loltracker.playerservices.domain.PlayerServiceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
class PlayerServicesControllerTest {

  private PlayerServicesController playerServicesController;

  @Mock private PlayerServiceUseCase playerServiceUseCase;

  @BeforeEach
  void setUp() {
    playerServicesController = new PlayerServicesController(playerServiceUseCase);
  }

  @Test
  void getSummoner() {
    when(playerServiceUseCase.getSummonerData("summonerName", "tagLine"))
        .thenReturn(Mono.just("Mocked response"));
    Mono<String> response = playerServicesController.getSummoner("summonerName", "tagLine");
    assertNotNull(response);
  }
}
