package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.domain.mappers.AccountMatchesMapper;
import com.loltracker.matchhistoryservice.infrastructure.models.AccountMatchesMO;
import com.loltracker.matchhistoryservice.infrastructure.models.MatchMO;
import com.loltracker.matchhistoryservice.infrastructure.repositories.AccountMatchesRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class MatchHistoryService {
  private final AccountMatchesRepository accountMatchesRepository;
  private final MatchAnalyzerService matchAnalyzerService;

  @Transactional
  public void processMatchHistory(AccountMatchesDTO matches) {
    try {
      processData(matches);
    } catch (Exception e) {
      log.error("Error saving match history", e);
      throw e;
    }
  }

  private void processData(AccountMatchesDTO matches) {
    accountMatchesRepository
        .findById(matches.getAccountDTO().getPuuid())
        .map(existing -> updateExisting(existing, matches))
        .orElseGet(() -> createNew(matches));
  }

  private AccountMatchesMO updateExisting(AccountMatchesMO existing, AccountMatchesDTO matches) {

    String gameName = matches.getAccountDTO().getGameName();
    List<MatchMO> incomingMatches = mapToMatchMos(matches);

    List<MatchMO> newMatches = getNewMatches(existing, incomingMatches);

    if (newMatches.isEmpty()) {
      log.info("No new matches to update for player {}", gameName);
      return existing;
    }
    return processNewMatches(existing, newMatches, gameName);
  }

  private List<MatchMO> mapToMatchMos(AccountMatchesDTO matches) {
    return matches.getMatchesDTO().getMatches().stream()
        .map(AccountMatchesMapper::toMatchMO)
        .toList();
  }

  private List<MatchMO> getNewMatches(AccountMatchesMO existing, List<MatchMO> incomingMatches) {
    return incomingMatches.stream()
        .filter(
            newMatch ->
                existing.getMatchesMO().getMatches().stream()
                    .noneMatch(
                        existingMatch ->
                            existingMatch
                                .getMetadata()
                                .getMatchId()
                                .equals(newMatch.getMetadata().getMatchId())))
        .toList();
  }

  private AccountMatchesMO processNewMatches(
      AccountMatchesMO existing, List<MatchMO> newMatches, String gameName) {
    matchAnalyzerService.analyzeMatches(gameName, newMatches);
    existing.getMatchesMO().getMatches().addAll(newMatches);
    log.info("Player {} matches updated in the database", gameName);
    return accountMatchesRepository.save(existing);
  }

  private AccountMatchesMO createNew(AccountMatchesDTO matches) {
    AccountMatchesMO entity = AccountMatchesMapper.toEntity(matches);
    matchAnalyzerService.analyzeMatches(
        entity.getAccountMO().getGameName(), entity.getMatchesMO().getMatches());
    return accountMatchesRepository.save(entity);
  }
}
