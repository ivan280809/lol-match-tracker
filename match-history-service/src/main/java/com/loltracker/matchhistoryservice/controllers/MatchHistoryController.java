package com.loltracker.matchhistoryservice.controllers;

import com.loltracker.matchhistoryservice.controllers.model.MatchesDTO;
import com.loltracker.matchhistoryservice.domain.MatchHistoryUseCase;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/match-history")
@AllArgsConstructor
public class MatchHistoryController {

  private final MatchHistoryUseCase matchHistoryUseCase;

  @PostMapping("/createMatchHistory/{puuid}")
  public Mono<String> getMatchHistory(
      @PathVariable("puuid") String puuid, @RequestBody MatchesDTO matches) {
    matchHistoryUseCase.processMatchHistory(puuid, matches);
    return Mono.just(matches.toString() + puuid);
  }
}
