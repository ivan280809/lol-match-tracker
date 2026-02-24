package com.loltracker.matchhistoryservice.controllers;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.domain.MatchHistoryUseCase;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/match-history")
@AllArgsConstructor
public class MatchHistoryController {

  private final MatchHistoryUseCase matchHistoryUseCase;

  @PostMapping("/createMatchHistory")
  public Mono<String> processMatches(@RequestBody AccountMatchesDTO matches) {
    matchHistoryUseCase.processMatchHistory(matches);
    return Mono.just(matches.getAccountDTO().getGameName());
  }

  @GetMapping("/puuid/{puuid}/matchId/{matchId}/exists")
  public Mono<Boolean> matchExists(
      @PathVariable("puuid") String puuid, @PathVariable("matchId") String matchId) {
    return matchHistoryUseCase.matchExists(puuid, matchId);
  }
}
