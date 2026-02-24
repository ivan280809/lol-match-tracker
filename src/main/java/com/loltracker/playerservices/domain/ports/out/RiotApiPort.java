package com.loltracker.playerservices.domain.ports.out;

import reactor.core.publisher.Mono;

public interface RiotApiPort {
  Mono<String> getSummonerByNameAndTagLine(String summonerName, String tagLine);

  Mono<String> getMatchesByPuuid(String puuid);

  Mono<String> getMatchById(String matchId);
}
