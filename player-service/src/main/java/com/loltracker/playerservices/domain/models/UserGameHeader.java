package com.loltracker.playerservices.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGameHeader implements Serializable {
  private String puuid;
  private String gameName;
  private String tagLine;
}
