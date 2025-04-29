package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Account")
@NoArgsConstructor
@AllArgsConstructor
public class AccountMO {
  @Id @GeneratedValue private Long id;

  private String puuid;
  private String gameName;
  private String tagLine;
}
