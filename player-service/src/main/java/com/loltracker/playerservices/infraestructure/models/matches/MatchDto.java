package com.loltracker.playerservices.infraestructure.models.matches;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchDto {
    private MetadataDto metadata;
    private InfoDto info;
}
