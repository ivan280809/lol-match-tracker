package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Account")
@NoArgsConstructor
@AllArgsConstructor
public class AccountMO {

  @Id
  @Column(name = "id")
  private String puuid;

  private String gameName;
  private String tagLine;

  @OneToOne(mappedBy = "accountMO")
  private AccountMatchesMO accountMatchesMO;
}
