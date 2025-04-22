package com.loltracker.playerservices.controllers;

import com.loltracker.playerservices.webclient.RiotApiClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/riot")
public class RiotController {

    private final RiotApiClient riotApiClient;

    public RiotController(RiotApiClient riotApiClient) {
        this.riotApiClient = riotApiClient;
    }

    @GetMapping("/summoner/{name}")
    public Mono<String> getSummoner(@PathVariable("name") String summonerName) {
        return riotApiClient.getSummonerByPUUID(summonerName);
    }
}
