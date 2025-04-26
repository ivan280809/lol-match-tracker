package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MatchMO {

  @Id private String matchId;
}
