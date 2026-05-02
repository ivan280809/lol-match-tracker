# Riot Player Platform Design

## Context

The dashboard currently asks for `gameName` and `tagLine`. Users can reasonably interpret `EUW` as the player's server, but the Riot Account API interprets it as the Riot ID tag in `gameName#tagLine`.

## Decision

Add a first-class player platform field separate from the Riot ID tag.

- Keep global Riot regional routing for Account-V1 and Match-V5 calls.
- Add a player-level platform selector for League platform routing such as `EUW1`, `NA1`, and `EUN1`.
- Store the platform on each player.
- Keep `tagLine` required because Riot Account-V1 still requires the Riot ID tag to resolve a PUUID.
- Update UI copy so `Tag` is no longer confused with server/region.

## Data Flow

1. The operator chooses a platform, for example `EUW`.
2. The operator enters Riot ID name and tag.
3. The backend resolves the PUUID through Account-V1 using configured regional routing.
4. The backend stores `gameName`, `tagLine`, `platform`, `puuid`, and active state.
5. Polling continues to use Match-V5 regional routing by PUUID.

Duplicate detection remains based on `gameName#tagLine` because the current Riot Account-V1 lookup resolves a global PUUID from the Riot ID. Platform is stored as operational context for League-specific routing, not as a second identity key yet.

## Error Handling

- If Riot cannot find `gameName#tagLine`, show the Riot ID in the error message.
- The platform selector must not imply that the tag can be omitted.

## Validation

- Controller tests must verify the new field is rendered and posted.
- Service tests must verify platform persistence and duplicate detection.
- Local Docker verification must confirm the UI still posts correctly.
