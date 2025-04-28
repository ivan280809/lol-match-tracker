package com.loltracker.playerservices.infraestructure.models.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerkStyleSelectionDto {
    private int perk;
    private int var1;
    private int var2;
    private int var3;
}
