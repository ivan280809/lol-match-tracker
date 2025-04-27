package com.loltracker.matchhistoryservice.controllers;

import com.loltracker.matchhistoryservice.controllers.model.MatchesDTO;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/match-history")

public class MatchHistoryController {

  @PostMapping("/createMatchHistory/{puuid}")
  public Mono<String> getMatchHistory( @PathVariable("puuid") String puuid, @RequestBody MatchesDTO matches) {
    return Mono.just(matches.toString()+puuid);
  }

  @GetMapping("/ping")
  public Mono<String> get() {
    return Mono.just("pong");
  }
}
