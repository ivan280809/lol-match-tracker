# Round 03 Cohesion Analysis

## Scope

Check how player platform selection fits existing module boundaries.

## Findings

- Player identity belongs in the `player` module.
- Riot regional routing belongs in `settings`.
- Riot API access remains behind `RiotClient`.

## Recommendations

- Put `RiotPlatform` with player model unless it becomes a broader integration concept.
- Do not move global region settings into player configuration.
- Do not add internal HTTP calls.

## Risks

- Mixing `region` and `platform` terminology can confuse operators.

## Handoff Items

- Use labels `Region Riot API` and `Servidor` consistently.
