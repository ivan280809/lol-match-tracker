package com.loltracker.playerservices.infraestructure.models.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerkStatsDto {
    private int defense;
    private int flex;
    private int offense;
}
