# Validation Report

## Status

Validated locally. P0 and P1 were implemented; P2 and P3 were documented as follow-up roadmaps.

## Implemented

- P0: Riot rank refresh now calls League-V4 directly by PUUID at `/lol/league/v4/entries/by-puuid/{encryptedPUUID}`.
- P0: removed the extra Summoner-V4 lookup from rank refresh, reducing rank refresh from two Riot calls to one.
- P0: added adapter tests for direct by-PUUID rank lookup, transient retry, and malformed rank responses.
- P1: notification rank stats now distinguish current rank, stored rank, unranked/no SoloQ/Flex rank, missing rank, stored-after-refresh-error, and refresh-error-without-stored-rank.
- P1: Telegram copy avoids raw exception text and adds concise rank notes such as `actualizado ahora`, `guardado`, `sin SoloQ/Flex`, `guardado; no se pudo actualizar`, and `no se pudo consultar Riot`.
- P2: documented API-backed V2 roadmap for Data Dragon/static metadata, LoL status diagnostics, Spectator live game state, champion mastery, Match-V5 timeline, and Challenges.
- P3: documented deferred ideas for Clash, tournaments, RSO, replays, free champion rotation, League-Exp imports, and low-priority UI ideas.

## Subagent Outcomes

- P0 worker changed `RiotClient` and `RiotClientTest`; focused and full tests passed in the worker run.
- P1 worker changed rank availability semantics and notification tests; focused and full tests passed in the worker run.
- P2 worker added `07-riot-api-p2-roadmap.md`; documentation-only.
- P3 worker added `08-riot-api-p3-deferred.md`; documentation-only.

## Command Outcomes

- `.\mvnw.cmd "-Dtest=RiotClientTest,PlayerRankServiceTest,NotificationStatsServiceTest,NotificationMessageFactoryTest,NotificationServiceTest" test`
  - Result: PASS.
  - Summary: 24 tests run, 0 failures, 0 errors, 0 skipped.
  - Finished: 2026-05-07 00:03 Europe/Madrid.
- `.\mvnw.cmd test`
  - Result: PASS.
  - Summary: 198 tests run, 0 failures, 0 errors, 2 skipped.
  - Skipped: `PostgreSqlPersistenceTest` 2 tests, because Docker/Testcontainers is unavailable locally.
  - Finished: 2026-05-07 00:04 Europe/Madrid.
- `.\mvnw.cmd -DskipTests package`
  - Result: PASS.
  - Artifact: `target\lol-match-tracker-1.0.0.jar`.
  - Finished: 2026-05-07 00:04 Europe/Madrid.

## Notes

- Test logs include intentional warning stack traces from mocked Telegram/Riot failure paths. Those warnings validate fallback behavior and did not fail the build.
- Maven/JDK warnings remain the same non-blocking local Java 25/Mockito/Jansi warnings already seen in previous iterations.
- PostgreSQL Testcontainers coverage remains skip-safe locally and should execute in Docker-enabled CI.

## Final Distributable Readiness Review

### P0 - Blocks Distribution

- None remaining from this iteration after local validation.
- The rank endpoint bug that caused Telegram messages to arrive without rank was fixed and covered by tests.

### P1 - Necessary For A Serious V2

- Mirror the new rank availability wording in the dashboard/player detail UI so Telegram and UI explain rank freshness consistently.
- Keep CI Docker-enabled so PostgreSQL/Testcontainers tests execute instead of skipping.
- Add an operator-facing rank refresh action if users need to verify a player's rank without waiting for a match notification.

### P2 - Important Improvements

- Implement `LeagueStaticMetadataPort` and cached Data Dragon/static metadata first.
- Add `RiotStatusPort` and dashboard diagnostics for Riot incidents/maintenance.
- Add Spectator, champion mastery, timeline, and challenges only behind explicit ports, TTLs, and mocked tests.
- Consider a rank refresh TTL/cache if notification volume grows.

### P3 - Nice To Have

- Clash, tournament, RSO, replay, free rotation, and League-Exp import ideas remain deferred.
- Promote League-Exp imports only after preview, dedupe, notification-suppression, rate-limit, and rollback design.

## Residual Risks

- Stored rank can still be stale when Riot rank refresh fails, but Telegram now says so.
- An actual player with no SoloQ/Flex entries is displayed as `Unranked` with the note `sin SoloQ/Flex`; product copy can be localized further later.
- Docker was unavailable locally, so PostgreSQL Testcontainers still need CI confirmation for this exact commit.
