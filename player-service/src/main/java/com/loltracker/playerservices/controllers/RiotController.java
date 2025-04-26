package com.loltracker.playerservices.controllers;

import com.loltracker.playerservices.domain.PlayerServiceUseCase;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@RequestMapping("/api/riot")
public class RiotController {

  private final PlayerServiceUseCase playerServiceUseCase;

  @GetMapping("/summoner/{name}/{tagLine}")
  public Mono<String> getSummoner(
      @PathVariable("name") String summonerName, @PathVariable("tagLine") String tagLine) {
    return playerServiceUseCase.getSummoner(summonerName, tagLine);
  }
}
