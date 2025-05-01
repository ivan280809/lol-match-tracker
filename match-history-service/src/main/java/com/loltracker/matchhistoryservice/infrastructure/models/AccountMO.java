package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Account")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AccountMO {

  @Id private String puuid;

  private String gameName;
  private String tagLine;

  @OneToOne(mappedBy = "accountMO")
  private AccountMatchesMO accountMatchesMO;
}
