package com.loltracker.playerservices.infraestructure.webclients;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Slf4j
public class RiotApiClient {

  private final WebClient webClient;
  private final Semaphore rateLimiter;
  private static final int PERMITS = 10;

  public RiotApiClient(
      @Value("${riot.api.key}") String apiKey, @Value("${riot.api.base-url}") String baseUrl) {

    this.webClient =
        WebClient.builder().baseUrl(baseUrl).defaultHeader("X-Riot-Token", apiKey).build();

    this.rateLimiter = new Semaphore(PERMITS);

    Schedulers.parallel()
        .schedulePeriodically(
            () -> {
              int permitsToRelease = 10 - rateLimiter.availablePermits();
              if (permitsToRelease > 0) {
                rateLimiter.release(permitsToRelease);
              }
            },
            0,
            10,
            TimeUnit.MILLISECONDS);
  }

  private <T> Mono<T> rateLimited(Mono<T> originalCall) {
    return Mono.defer(
        () -> {
          if (rateLimiter.tryAcquire()) {
            return originalCall;
          } else {
            return Mono.delay(Duration.ofMillis(1000)).flatMap(tick -> rateLimited(originalCall));
          }
        });
  }

  public Mono<String> getSummonerByNameAndTagLine(String summonerName, String tagLine) {
    return rateLimited(
        webClient
            .get()
            .uri("riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}", summonerName, tagLine)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSubscribe(
                s -> log.info("Fetching summoner: {} with tag: {}", summonerName, tagLine))
            .doOnError(
                error ->
                    log.warn(
                        "Error fetching summoner: {} with tag: {}", summonerName, tagLine, error)));
  }

  public Mono<String> getMatchesByPuuid(String puuid) {
    return rateLimited(
        webClient
            .get()
            .uri("lol/match/v5/matches/by-puuid/{puuid}/ids", puuid)
            .retrieve()
            .bodyToMono(String.class));
  }

  public Mono<String> getMatchById(String matchId) {
    return rateLimited(
        webClient
            .get()
            .uri("/lol/match/v5/matches/{matchId}", matchId)
            .retrieve()
            .bodyToMono(String.class));
  }
}
