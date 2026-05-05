# Validation Report

## Status

Implemented and validated as a strong Round 09 V2 block, with explicit residual gaps for a final distributable release.

This iteration should not be described as the complete V2 finish line. It does deliver the reliable Riot/polling/backfill/global-match foundation, but production-safe migrations, browser E2E, full read-model migration, and shared Telegram aggregation remain open.

## Implemented

- Replaced reactive `WebClient`/`.block()` integration flow with blocking Spring `RestClient` adapters.
- Added explicit integration ports:
  - `RiotAccountPort`
  - `RiotMatchPort`
  - `RiotRankPort`
  - `RiotOperationsPort`
  - `TelegramNotificationPort`
- Added Riot error classification for:
  - missing config,
  - invalid API key / unauthorized,
  - not found,
  - rate limit with `Retry-After`,
  - wrong region/platform,
  - timeout/network failure,
  - malformed response,
  - transient 5xx,
  - unknown failures.
- Added product-facing Riot messages for polling/player sync instead of surfacing raw exception text.
- Added basic retry with backoff for timeout/network/5xx and no immediate retry for functional 4xx or 429.
- Added injectable `Clock` and configurable app `ZoneId`.
- Added persisted polling config with env fallback:
  - enabled/disabled,
  - manual-only,
  - fixed delay,
  - match window size,
  - pagination limit.
- Added DB poll lock with lease and owner token.
- Added active polling dashboard state:
  - active run,
  - current player,
  - current stage,
  - rate-limit pause,
  - disabled manual run button when a run/pause is active.
- Added `track_from` and backfill mode for players.
- Defaulted new players to notify only matches after creation.
- Added explicit backfill import without Telegram notification spam.
- Added Match-V5 pagination and per-poll-run match detail cache.
- Added global `MatchEntity` and `PlayerMatchEntity` with dual-write from legacy `TrackedMatchEntity`.
- Expanded reduced match stats: champion id/name, queue id, lane/role, KDA, CS, gold, damage, vision, duration, result, game end time, platform, and region.
- Added service-level duplicate PUUID rejection.
- Updated Docker Compose, deploy Compose, `.env.example`, and README with the new operational settings.

## Validation Commands

### `.\mvnw.cmd test`

Result: passed.

Observed result:

- `BUILD SUCCESS`
- `Tests run: 176`
- `Failures: 0`
- `Errors: 0`
- `Skipped: 0`

Coverage added/confirmed:

- Riot adapter tests for 429 + `Retry-After`, timeout, 401/403, 404, 5xx retry, and malformed response.
- Polling service unit tests for normal runs, partial failure, local concurrency skip, failure cleanup, and pending notification behavior.
- Integration tests with H2 and mocked Riot/Telegram ports.
- Backfill test proving historical matches import with notifications suppressed.
- Match + PlayerMatch dedupe test proving one global match and one participation per player.
- Shared-match polling test proving one full Riot match detail fetch is reused across tracked players in the same game.
- Persistent poll lock test proving DB lock acquisition, rejection, persistence, release, and reacquisition.
- MVC dashboard tests for active run display, rate-limit pause display, disabled manual polling, and polling config form persistence.

Notes:

- Maven/JDK emitted native-access/deprecation warnings from build tooling.
- Mockito emitted dynamic-agent warnings on Java 25. These are not product failures but should be addressed in CI hygiene.

### `.\mvnw.cmd -DskipTests package`

Result: passed.

Observed result:

- `BUILD SUCCESS`
- Repackaged Spring Boot jar created at `target/lol-match-tracker-1.0.0.jar`.

## Full-Code Inspection

Searches performed across main source, build files, docs, Docker, and CI:

- No remaining `WebClient`, WebFlux/Reactor dependency, `.block()`, `Mono`, or `Flux` usage in production source or `pom.xml`.
- External calls are now behind blocking ports/adapters.
- Major polling/notification/player service timestamps use injected `Clock`; remaining `Instant.now()` calls are in JPA entity lifecycle callbacks, view duration fallback, or backward-compatible test constructors.
- `Europe/Madrid` remains only as default config/compatibility fallback, not as the only production behavior.
- Docker and deploy Compose now expose polling, retry, timeout, and timezone env knobs.
- Existing CI still runs tests before publishing GHCR images.

## Browser / E2E Gap

The repository still has no browser automation infrastructure. This round includes Spring MVC tests and H2-backed end-to-end-style integration tests without real Riot/Telegram calls, but it does not include browser-driven E2E tests.

This remains a validation gap against the repository's stated full validation standard.

## Final Distributable Readiness Review

### P0 Blocks Distribution

- Add versioned DB migrations with Flyway or Liquibase. Current production schema changes still depend on `spring.jpa.hibernate.ddl-auto=update`, which is not acceptable as the final migration strategy.
- Add a safe historical migration/backfill from existing `tracked_matches` into `matches` and `player_matches`. Round 09 dual-writes new data but does not convert all legacy history.
- Create explicit SQL indexes/constraints in migrations, including global match identity, player participation, poll locks, poll runs, outbox retry lookup, player PUUID, archive/filter columns, and match end time.

### P1 Necessary For A Serious V2

- Add browser-driven E2E tests for dashboard polling controls, player create/edit/backfill, health actions, detail pages, archive/restore, and audit navigation.
- Add PostgreSQL integration tests with Testcontainers. H2 is useful, but it does not prove locking/index/migration behavior on the production database.
- Finish the read-model migration from `tracked_matches` to `matches`/`player_matches` for dashboard stats, player detail, notifications, and shared-match statistics.
- Add a DB-level PUUID uniqueness strategy after duplicate cleanup. Round 09 enforces duplicate PUUIDs in service logic only.
- Add minimal operational access control if the app is exposed outside a trusted LAN, even if broader security remains out of scope.
- Add an explicit production migration/runbook for operators upgrading from the current schema.

### P2 Important Improvements

- Add shared aggregated Telegram notification cards. Round 09 avoids duplicate Riot match fetches and supports shared match storage, but keeps individual notifications for compatibility.
- Convert remaining string statuses to enums where safe: poll run status, player sync status, legacy tracked match result.
- Move remaining entity lifecycle timestamps to a consistent auditing strategy if deterministic entity timestamps become important.
- Add Micrometer counters/timers for poll runs, Riot calls, 429 pauses, retries, notification sends/failures, and outbox backlog.
- Add Docker app-level healthcheck and resource recommendations.
- Add CI artifact retention for packaged jar and test reports.
- Address Java 25 Mockito dynamic-agent warnings in test runtime configuration.

### P3 Nice To Have

- Add queue type display mapping beyond numeric queue id.
- Add richer shared-match dashboard statistics.
- Add operator-facing copy for multi-region limitations and Riot regional cluster choices.
- Add rate-limit history charts in the audit/health area.

## Residual Risks

- Dual-write keeps legacy and V2 match models in parallel. New data is covered, but existing history needs an explicit migration.
- A global Riot 429 pause is intentionally conservative; it may delay players on unaffected routes but protects the app from worsening Riot pressure.
- Persisted polling config and env fallback are implemented, but operators need documented upgrade guidance for existing deployments.
- JPA lifecycle timestamps still use system time directly in entities.
- No real Riot or Telegram calls are made in tests; adapter behavior is covered with mocks/test server.

## Recommendation For Next Iteration

Make Round 10 a release-hardening/migration iteration:

1. Introduce Flyway/Liquibase and set `ddl-auto=validate` for production.
2. Write the legacy `tracked_matches` to `matches`/`player_matches` migration.
3. Add PostgreSQL Testcontainers coverage for migrations, poll locks, dedupe, and indexes.
4. Add browser E2E for the core dashboard/operator flows.
5. Switch dashboard/detail/read stats to the global match model.
6. Decide whether shared Telegram aggregation becomes default or opt-in.
