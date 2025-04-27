package com.loltracker.playerservices.application.controllers;

import com.loltracker.playerservices.domain.PlayerServiceUseCase;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@RequestMapping("/player/services")
public class PlayerServicesController {

  private final PlayerServiceUseCase playerServiceUseCase;

  @GetMapping("/summoner/{name}/{tagLine}")
  public Mono<String> getSummoner(
      @PathVariable("name") String summonerName, @PathVariable("tagLine") String tagLine) {
    return playerServiceUseCase.getSummonerData(summonerName, tagLine);
  }
}
