package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.domain.mappers.AccountMatchesMapper;
import com.loltracker.matchhistoryservice.infrastructure.repositories.AccountMatchesRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class MatchHistoryService {
  private final AccountMatchesRepository accountMatchesRepository;

  public void processMatchHistory(AccountMatchesDTO matches) {
    try {
      if (!accountMatchesRepository.findById(matches.getAccountDTO().getPuuid()).isPresent()) {
        accountMatchesRepository.save(AccountMatchesMapper.toEntity(matches));
      }
    } catch (Exception e) {
      log.error("Error saving match history", e);
      throw e;
    }
  }
}
