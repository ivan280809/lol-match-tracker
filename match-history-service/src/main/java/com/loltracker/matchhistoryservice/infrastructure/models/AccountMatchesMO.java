package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Account_Matches")
@NoArgsConstructor
@AllArgsConstructor
public class AccountMatchesMO {
  @Id @GeneratedValue private Long id;

  @OneToOne private AccountMO accountMO;

  @OneToOne private MatchesMO matchesMO;
}
