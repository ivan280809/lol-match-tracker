package com.loltracker.matchhistoryservice.controllers.model;

import com.loltracker.matchhistoryservice.controllers.model.account.AccountDTO;
import com.loltracker.matchhistoryservice.controllers.model.matches.MatchesDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountMatchesDTO {
  private AccountDTO accountDTO;
  private MatchesDTO matchesDTO;
}
