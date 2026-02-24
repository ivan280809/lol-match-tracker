package com.loltracker.playerservices.infrastructure.models;

import com.loltracker.playerservices.infrastructure.models.account.AccountDTO;
import com.loltracker.playerservices.infrastructure.models.matches.MatchesDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountMatchesDTO {
  private AccountDTO accountDTO;
  private MatchesDTO matchesDTO;
}
