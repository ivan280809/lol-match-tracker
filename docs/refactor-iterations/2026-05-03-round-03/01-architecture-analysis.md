# Round 03 Architecture Analysis

## Scope

Add player-level Riot platform selection while keeping the application a modular monolith.

## Findings

- Riot regional routing is currently global configuration.
- Player identity currently stores only Riot ID name, Riot ID tag, and PUUID.
- The UI label `Tag` can be confused with platform names like `EUW`.

## Recommendations

- Add a `RiotPlatform` enum in the player boundary.
- Persist platform on `players`.
- Keep Account-V1 resolution unchanged because it requires `gameName#tagLine`.
- Make the UI explicit: platform is server, tag is Riot ID tag.

## Risks

- Existing rows need a default platform.
- Users may still expect server-only lookup by old summoner name, which Riot Account-V1 does not provide.

## Handoff Items

- Add persistence column with default handling.
- Update form, view, controller model, and tests.
