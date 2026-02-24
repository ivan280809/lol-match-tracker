package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Account_Matches")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AccountMatchesMO {
  @Id private String puuid;

  @OneToOne(cascade = CascadeType.PERSIST)
  @MapsId
  @JoinColumn(name = "puuid")
  private AccountMO accountMO;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "matches_id")
  private MatchesMO matchesMO;
}
