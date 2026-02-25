package com.loltracker.playerservices.domain.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.loltracker.playerservices.domain.ports.out.MatchHistoryPort;
import com.loltracker.playerservices.domain.ports.out.RiotApiPort;
import com.loltracker.playerservices.infrastructure.models.account.AccountDTO;
import com.loltracker.playerservices.infrastructure.models.matches.MatchDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PlayerDataServiceTest {

  @Mock private RiotApiPort riotApiPort;

  @Mock private MatchHistoryPort matchHistoryPort;

  @Mock private ObjectMapper objectMapper;

  private PlayerDataService playerDataService;

  @BeforeEach
  void setUp() {
    playerDataService = new PlayerDataService(riotApiPort, matchHistoryPort, objectMapper);
  }

  @Test
  void loadPlayers_ShouldInitializeService() {
    ObjectMapper realMapper = new ObjectMapper();
    playerDataService = new PlayerDataService(riotApiPort, matchHistoryPort, realMapper);
    verifyNoInteractions(riotApiPort, matchHistoryPort);
  }

  @Test
  void getSummonerData_ShouldReturnMatchData() throws Exception {
    String summonerResponse = "{\"puuid\":\"puuid-123\"}";
    String matchIdsJson = "[\"m1\"]";
    String matchJson = "{\"gameId\":1}";

    when(riotApiPort.getSummonerByNameAndTagLine(anyString(), anyString()))
        .thenReturn(Mono.just(summonerResponse));
    when(riotApiPort.getMatchesByPuuid(anyString())).thenReturn(Mono.just(matchIdsJson));
    when(riotApiPort.getMatchById(anyString())).thenReturn(Mono.just(matchJson));
    when(matchHistoryPort.putMatches(any())).thenReturn(Mono.just("OK"));
    when(matchHistoryPort.matchExists(anyString(), anyString())).thenReturn(Mono.just(false));

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
