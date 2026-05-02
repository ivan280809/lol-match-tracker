# Round 03 Implementation Plan

## Steps

1. Add `RiotPlatform` enum.
2. Add `platform` to `PlayerEntity`, `PlayerForm`, and `PlayerView`.
3. Include platform in duplicate detection.
4. Render platform selector on the dashboard.
5. Update tests.
6. Run local test suite and Docker verification.

## Validation Targets

- Unit and controller tests pass.
- Dashboard renders platform selector.
- Posting a player sends `platform`, `gameName`, and `tagLine`.
