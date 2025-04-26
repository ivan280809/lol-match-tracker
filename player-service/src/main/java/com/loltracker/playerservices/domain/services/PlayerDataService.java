package com.loltracker.playerservices.domain.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.domain.exceptions.UserNotFoundException;
import com.loltracker.playerservices.domain.models.MatchesIds;
import com.loltracker.playerservices.domain.models.UserGameHeader;
import com.loltracker.playerservices.webclient.RiotApiClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Service
@AllArgsConstructor
public class PlayerDataService {

    private final RiotApiClient riotApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public Mono<String> getSummoner(String summonerName, String tagLine) {
        return riotApiClient
                .getSummonerByNameAndTagLine(summonerName, tagLine)
                .flatMap(handleSummonerDetails())
                .onErrorResume(handleError());
    }

    private Function<String, Mono<? extends String>> handleSummonerDetails() {
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
        UserGameHeader userGameHeader = objectMapper.readValue(response, UserGameHeader.class);
        getMatchesByPuuid(userGameHeader);
        return Mono.just(userGameHeader.toString());
    }

    private Mono<List<String>> getMatchesByPuuid(UserGameHeader userGameHeader) {
        return riotApiClient.getMatchesByPuuid(userGameHeader.getPuuid())
                .map(this::parseMatches);
    }

    private List<String> parseMatches(String response) {
        try {
            MatchesIds matchesIds = objectMapper.readValue(response, MatchesIds.class);
            return matchesIds.getMatches();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing MatchesIds", e);
        }
    }


    private Function<Throwable, Mono<? extends String>> handleError() {
        return e -> Mono.error(new UserNotFoundException("User not found"));
    }
}
