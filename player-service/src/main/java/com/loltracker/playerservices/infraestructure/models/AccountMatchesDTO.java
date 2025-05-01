package com.loltracker.playerservices.infraestructure.models;

import com.loltracker.playerservices.infraestructure.models.account.AccountDTO;
import com.loltracker.playerservices.infraestructure.models.matches.MatchesDTO;
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
