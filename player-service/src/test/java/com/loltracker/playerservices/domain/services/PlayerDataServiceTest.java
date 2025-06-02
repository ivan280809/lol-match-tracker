package com.loltracker.playerservices.domain.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loltracker.playerservices.infrastructure.models.account.AccountDTO;
import com.loltracker.playerservices.infrastructure.models.matches.MatchDto;
import com.loltracker.playerservices.infrastructure.webclients.MatchServiceWebClient;
import com.loltracker.playerservices.infrastructure.webclients.RiotApiClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PlayerDataServiceTest {

  @Mock private RiotApiClient riotApiClient;

  @Mock private MatchServiceWebClient matchServiceWebClient;

  @Mock private ObjectMapper objectMapper;

  private PlayerDataService playerDataService;

  @BeforeEach
  void setUp() {
    playerDataService = new PlayerDataService(riotApiClient, matchServiceWebClient, objectMapper);
  }

  @Test
  void loadPlayers_ShouldLoadNonEmptyList() {
    ObjectMapper realMapper = new ObjectMapper();
    playerDataService = new PlayerDataService(riotApiClient, matchServiceWebClient, realMapper);

    playerDataService.refreshPlayerData();
  }

  @Test
  void getSummonerData_ShouldReturnMatchData() throws Exception {
    String summonerResponse = "{\"puuid\":\"puuid-123\"}";
    String matchIdsJson = "[\"m1\"]";
    String matchJson = "{\"gameId\":1}";

    when(riotApiClient.getSummonerByNameAndTagLine(anyString(), anyString()))
        .thenReturn(Mono.just(summonerResponse));
    when(riotApiClient.getMatchesByPuuid(anyString())).thenReturn(Mono.just(matchIdsJson));
    when(riotApiClient.getMatchById(anyString())).thenReturn(Mono.just(matchJson));
    when(matchServiceWebClient.putMatches(any())).thenReturn(Mono.just("OK"));
    when(matchServiceWebClient.matchExists(anyString(), anyString())).thenReturn(Mono.just(false));

    AccountDTO acct = new AccountDTO();
    acct.setPuuid("puuid-123");
    doReturn(acct).when(objectMapper).readValue(eq(summonerResponse), eq(AccountDTO.class));

    doReturn(List.of("m1"))
        .when(objectMapper)
        .readValue(eq(matchIdsJson), ArgumentMatchers.<TypeReference<List<String>>>any());

    doReturn(new MatchDto()).when(objectMapper).readValue(eq(matchJson), eq(MatchDto.class));

    StepVerifier.create(playerDataService.fetchAccountAndStoreMatches("foo", "bar"))
        .expectNext("OK")
        .verifyComplete();
  }
}
