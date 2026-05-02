# Round 03 Decisions

## Inputs

- User request on 2026-05-03 to add support for selecting EUW separately after testing `bazaga#euw` returned Riot `404`.
- Design in `docs/plans/2026-05-03-riot-player-platform-design.md`.

## Decisions

- Add player-level platform selection.
- Store platform as an enum string in `players.platform`.
- Default existing and new players to `EUW1`.
- Keep `tagLine` required.
- Keep duplicate detection based on `gameName#tagLine`.
- Keep Riot regional route configuration global.
- Keep polling behavior unchanged for now.

## Rejected Alternatives

- Treating `EUW` as the Riot ID tag: rejected because Riot Account-V1 interprets it as a tag and returned not found.
- Free-form platform text: rejected because Riot platforms are bounded constants.
- Looking up by summoner name only: rejected because the current Riot integration is Account-V1 and needs Riot ID tag.

## Consequences

- Users must enter the real Riot ID tag and choose server separately.
- Future platform-specific endpoints can use the stored platform.
