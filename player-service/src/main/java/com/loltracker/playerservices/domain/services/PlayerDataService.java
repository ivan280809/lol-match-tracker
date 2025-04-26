package com.loltracker.playerservices.domain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.exceptions.UserNotFoundException;
import com.loltracker.playerservices.domain.models.UserGameHeader;
import com.loltracker.playerservices.webclient.RiotApiClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Service
@AllArgsConstructor
public class PlayerDataService {

    private final RiotApiClient riotApiClient;

    public Mono<String> getSummoner(String summonerName, String tagLine) {
        return riotApiClient
                .getSummonerByNameAndTagLine(summonerName, tagLine)
                .flatMap(handleResponse())
                .onErrorResume(handleError());
    }

    private Function<String, Mono<? extends String>> handleResponse() {
        return response -> {
            try {
                return processUser(response);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Mono<String> processUser(String response)
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        UserGameHeader userGameHeader = objectMapper.readValue(response, UserGameHeader.class);
        return Mono.just(userGameHeader.toString());
    }

    private Function<Throwable, Mono<? extends String>> handleError() {
        return e -> Mono.error(new UserNotFoundException("User not found"));
    }
}
