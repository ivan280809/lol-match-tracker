package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Account_Matches")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountMatchesMO {
  @Id @GeneratedValue private Long id;

  @OneToOne
  @JoinColumn(name = "account_id", referencedColumnName = "id")
  private AccountMO accountMO;

  @OneToOne
  @JoinColumn(name = "matches_id")
  private MatchesMO matchesMO;
}
