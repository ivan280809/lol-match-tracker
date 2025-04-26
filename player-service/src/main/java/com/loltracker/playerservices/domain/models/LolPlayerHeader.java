package com.loltracker.playerservices.domain.models;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LolPlayerHeader implements Serializable {
  private String puuid;
  private String gameName;
  private String tagLine;
}
