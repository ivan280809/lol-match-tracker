package com.loltracker.matchhistoryservice.domain.services;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.domain.mappers.AccountMatchesMapper;
import com.loltracker.matchhistoryservice.infrastructure.repositories.AccountMatchesRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MatchHistoryService {
  private final AccountMatchesRepository accountMatchesRepository;
  private final AccountMatchesMapper accountMatchesMapper;

  public void processMatchHistory(AccountMatchesDTO matches) {
    accountMatchesRepository.save(accountMatchesMapper.toEntity(matches));
  }
}
