# Validation Report

## Status

Validated locally.

## Implemented

- Redesigned the Telegram message into compact sections: `Partida`, `Rendimiento`, `Forma`, optional `Compartida`, and `Rank`.
- Added persisted match data to Telegram:
  - KDA and KDA ratio.
  - CS and CS/min.
  - gold and gold/min.
  - damage to champions and damage/min.
  - vision score and vision/min.
  - queue label and queue id.
  - champion id when present.
  - lane/role.
  - platform/region.
  - match id.
- Added shared tracked-player context when several tracked players are in the same match.
- Kept Telegram HTML escaping for Riot/player-provided values.
- Made `MatchQueueCatalog` public so notification formatting reuses the existing queue catalog.
- Added a repository read for same-match tracked participants without changing outbox identity.

## Deferred

- Data Dragon icons/assets, Riot status, Spectator live state, mastery, timeline, challenges, RSO, replays, and imports were not added to notification dispatch.
- Reason: they require new external calls, caches, TTLs, and separate validation to avoid slowing or rate-limiting Telegram delivery.

## Command Outcomes

- `.\mvnw.cmd "-Dtest=NotificationMessageFactoryTest,NotificationStatsServiceTest,NotificationServiceTest" test`
  - Result: PASS.
  - Summary: 10 tests run, 0 failures, 0 errors, 0 skipped.
  - Finished: 2026-05-07 00:20 Europe/Madrid.
- `.\mvnw.cmd test`
  - Result: PASS.
  - Summary: 200 tests run, 0 failures, 0 errors, 2 skipped.
  - Skipped: `PostgreSqlPersistenceTest` 2 tests, because Docker/Testcontainers is unavailable locally.
  - Finished: 2026-05-07 00:20 Europe/Madrid.
- `.\mvnw.cmd -DskipTests package`
  - Result: PASS.
  - Artifact: `target\lol-match-tracker-1.0.0.jar`.
  - Finished: 2026-05-07 00:21 Europe/Madrid.

## Notes

- Test logs include intentional warnings from mocked failure paths. They validate retry/fallback behavior and do not indicate build failure.
- The local Java 25/Jansi/Mockito warnings remain non-blocking.

## Final Distributable Readiness Review

### P0 - Blocks Distribution

- None found after validation.

### P1 - Necessary For A Serious V2

- Deploy the richer Telegram message and confirm a real Telegram notification renders as expected on mobile.
- Add UI parity later so player detail exposes the same KDA/per-minute breakdown.

### P2 - Important Improvements

- Implement static Data Dragon metadata behind a cache before adding icons/assets to Telegram.
- Add aggregated shared Telegram notifications as a separate feature with outbox identity and duplicate-prevention tests.
- Add Riot Status diagnostics to explain failures before operators blame Telegram or polling.

### P3 - Nice To Have

- Add Spectator/mastery/timeline/challenges only after the P2 metadata/cache foundation exists.

## Residual Risks

- Shared-match context is informational only; notification outbox remains per tracked match.
- If old data lacks reduced fields, the message will show zeros or fallback labels for those old rows.
- Docker-backed PostgreSQL tests were skipped locally and should be checked by CI.
