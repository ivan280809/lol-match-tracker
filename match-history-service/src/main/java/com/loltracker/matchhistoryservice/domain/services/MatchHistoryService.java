package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.domain.mappers.AccountMatchesMapper;
import com.loltracker.matchhistoryservice.infrastructure.models.AccountMatchesMO;
import com.loltracker.matchhistoryservice.infrastructure.models.MatchMO;
import com.loltracker.matchhistoryservice.infrastructure.repositories.AccountMatchesRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class MatchHistoryService {
  private final AccountMatchesRepository accountMatchesRepository;

  public void processMatchHistory(AccountMatchesDTO matches) {
    try {
      processData(matches);
    } catch (Exception e) {
      log.error("Error saving match history", e);
      throw e;
    }
  }

  public void processData(AccountMatchesDTO matches) {
    String puuid = matches.getAccountDTO().getPuuid();
    List<MatchMO> incomingMatches = mapToMatchMos(matches);

    AccountMatchesMO entity = accountMatchesRepository.findById(puuid)
            .map(existing -> updateExisting(existing, incomingMatches, matches.getAccountDTO().getGameName()))
            .orElseGet(() -> createNew(matches));

    accountMatchesRepository.save(entity);
  }

  private List<MatchMO> mapToMatchMos(AccountMatchesDTO matches) {
    return matches.getMatchesDTO().getMatches()
            .stream()
            .map(AccountMatchesMapper::toMatchMO)
            .toList();
  }

  private AccountMatchesMO updateExisting(AccountMatchesMO existing,
                                        List<MatchMO> incomingMatches,
                                        String gameName) {

    List<MatchMO> newMatches = incomingMatches.stream().filter(
            newMatch -> existing.getMatchesMO().getMatches()
                    .stream()
                    .noneMatch(existingMatch -> existingMatch.getMetadata().getMatchId().equals(newMatch.getMetadata().getMatchId()))
    ).toList();
    // TODO: create listener with matchAnalyzerService
    existing.getMatchesMO()
            .getMatches()
            .addAll(newMatches);
    log.info("Player {} matches updated in the database", gameName);
    return existing;
  }

  private AccountMatchesMO createNew(AccountMatchesDTO matches) {
    return AccountMatchesMapper.toEntity(matches);
  }
}
