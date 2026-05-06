# Validation Report

## Status

Validated with environment-dependent gaps recorded.

## Scope

- Added skip-safe PostgreSQL Testcontainers coverage for poll locks and global match deduplication.
- Added server-rendered MockMvc E2E fallback for dashboard player creation, manual polling, player detail, and audit pages without live Riot or Telegram calls.
- Kept general Spring Boot tests on H2 `create-drop`; Flyway migration behavior is covered by the dedicated migration test.
- Added ui-ops coverage for optional dashboard Basic auth, low-cardinality Micrometer meters, rate-limit history rendering, Docker healthchecks, and CI artifact retention.

## Command Outcomes

- `.\mvnw.cmd test`
  - Result: PASS.
  - Summary: 190 tests run, 0 failures, 0 errors, 2 skipped.
  - Skipped: `PostgreSqlPersistenceTest` 2 tests, because Docker was unavailable.
  - Finished: 2026-05-06 23:08 Europe/Madrid after the CI hotfix below.
- `.\mvnw.cmd -DskipTests package`
  - Result: PASS.
  - Artifact: `target\lol-match-tracker-1.0.0.jar`.
  - Finished: 2026-05-06 23:07 Europe/Madrid after the CI hotfix below.

## Additional Focused Validation

- `.\mvnw.cmd "-Dtest=DashboardAccessGuardFilterTest,PostgreSqlPersistenceTest,FlywayMigrationTest,PersistenceIntegrationTest,DashboardMvcEndToEndFallbackTest,DashboardControllerTest" test`
  - Result: PASS.
  - Summary: 38 tests run, 0 failures, 0 errors, 2 skipped.
  - Purpose: guard, migrations, global match read model, PostgreSQL skip-safe coverage, MVC E2E fallback, dashboard/audit MVC contracts.
- `.\mvnw.cmd "-Dtest=PostgreSqlPersistenceTest,FlywayMigrationTest" test`
  - Result: PASS.
  - Summary: 4 tests run, 0 failures, 0 errors, 2 skipped on this workstation.
  - Purpose: validate the PostgreSQL/Flyway hotfix path remains skip-safe locally while Flyway scripts still run on H2.

## CI Hotfix

- GitHub Actions run `25461004571` failed in `PostgreSqlPersistenceTest` because Docker was available in CI, so the PostgreSQL Testcontainers tests executed and Hibernate `ddl-auto=validate` started before Flyway had created the schema.
- The test now applies Flyway migrations explicitly to the PostgreSQL container before the Spring context is created, then disables Spring-managed Flyway for that test context. This keeps the test strict: Hibernate validates the schema that the production migrations create.
- The corrected PostgreSQL validation ordering was confirmed in GitHub Actions run `25461254902`.

## CI And Deployment Validation

- GitHub Actions run `25461254902`
  - Result: PASS.
  - Commit: `70b1fa05dcef948035a9d2a2931413816112c06d`.
  - `test-and-publish`: PASS in 1m51s. Docker-enabled CI executed tests, packaged the application, uploaded artifacts, and pushed `ghcr.io/ivan280809/lol-match-tracker:70b1fa05dcef948035a9d2a2931413816112c06d`.
  - `deploy-minipc`: PASS in 25s. The mini PC pulled the image, recreated the Spring Boot container, kept PostgreSQL healthy, and exposed the app on `127.0.0.1:8085->8080/tcp`.
  - Healthcheck: PASS. `/actuator/health` returned `{"groups":["liveness","readiness"],"status":"UP"}`.
  - Non-blocking warning: GitHub Actions reported Node.js 20 action deprecation warnings for several marketplace actions. This does not block the current release, but should be addressed before GitHub's 2026 Node 20 removal deadlines.

## Code Inspection

- Production scan found no remaining `WebClient`, `.block()`, `Mono`, or `Flux` usage in `src/main/java`.
- Riot and Telegram calls are behind explicit ports/adapters: `RiotAccountPort`, `RiotMatchPort`, `RiotRankPort`, `RiotOperationsPort`, and `TelegramNotificationPort`.
- External calls in player creation/update, rank refresh, polling, validation, and notification dispatch are not wrapped in long service-level transactions. Persistence is performed through focused repository/service methods after external data is resolved.
- Remaining `tracked_matches` use is intentional compatibility for notification outbox and legacy fallback.

## Environment-Dependent Checks

- Docker probe: FAILED. `docker version --format '{{.Server.Version}}'` could not connect to `dockerDesktopLinuxEngine`; the named pipe was missing.
- PostgreSQL Testcontainers: ADDED and SKIPPED. `@Testcontainers(disabledWithoutDocker = true)` prevented local failures when Docker was unavailable.
- Browser tooling probe: FAILED. Python `playwright` module is not installed. Browser-style E2E was not added.
- MVC E2E fallback: ADDED and PASSED. `DashboardMvcEndToEndFallbackTest` ran 1 test with 0 failures.

## Findings

- The Maven build did not manage Testcontainers versions, so `org.testcontainers:junit-jupiter` and `org.testcontainers:postgresql` are pinned through `testcontainers.version`.
- Round 10 production changes introduced constructor/API drift in existing tests; test fixtures were updated for `AuditView` and `RiotClient`.
- The shared test profile cannot use JPA `validate` against empty H2 contexts unless Flyway is reliably applied before context validation. The suite now uses H2 `create-drop`, while `FlywayMigrationTest` validates the migration scripts directly.
- The dashboard guard is disabled by default and leaves `/actuator/health` open for Compose, Docker, and deploy checks.
- Metrics use operation/status/category/result labels only; no player, PUUID, match id, or free-form message tags were added.

## Recommendations

- Keep the Docker-enabled CI run as the release gate for PostgreSQL Testcontainers execution.
- Add real browser E2E only after the project owns repeatable browser tooling in the build, such as Playwright Java or another Maven-controlled runner.
- Keep migration tests separate from general H2 service tests unless the test profile is intentionally redesigned around Flyway-managed schemas.

## Risks

- P2: PostgreSQL behavior is covered by code but not executed on this workstation because Docker is unavailable.
- P2: Browser coverage remains a gap; MockMvc verifies rendered server flows but not browser layout, JavaScript, or CSS behavior.
- P3: Maven/JDK output includes warnings about future dynamic agent restrictions for Mockito/Byte Buddy on the local Java 25 runtime.

## Final Distributable Readiness Review

### P0 - Blocks Distribution

- None found in this iteration after validation. Unit/integration/MVC tests pass and the package builds.

### P1 - Necessary For A Serious V2

- Run the full suite in Docker-enabled CI/local environment and require the PostgreSQL Testcontainers tests to execute, not skip.
- Add an upgrade runbook for existing databases before production rollout, especially duplicate nonblank `players.puuid` cleanup and Flyway baseline expectations.
- Keep `APP_CONFIG_ENCRYPTION_KEY` documented as mandatory stable production configuration; losing it makes stored DB secrets unreadable.

### P2 - Important Improvements

- Add committed browser automation through Maven-managed tooling. Current MVC E2E fallback does not validate CSS/JS/layout.
- Move notification outbox identity from legacy `tracked_matches` to `player_matches` once shared notification aggregation is ready.
- Add true shared-match Telegram aggregation as an opt-in feature with duplicate prevention tests.
- Add Prometheus or another metrics exporter if production monitoring needs scraping beyond default Actuator JSON metrics.
- Reduce legacy fallback usage further after real deployment backfill has proven complete.
- Improve notification exactly-once semantics. Current outbox is at-least-once; a crash after Telegram success and before DB `SENT` can still retry.

### P3 - Nice To Have

- Replace remaining string statuses with enums where the blast radius is low: player sync status and poll status.
- Tune Java 25/Mockito dynamic-agent warnings in the build.
- Add richer queue catalog coverage as Riot adds new queues.

## Implemented In Round 10

- Flyway production schema ownership with `ddl-auto=validate`.
- Current schema migration and idempotent legacy `tracked_matches` to `matches`/`player_matches` backfill.
- Global match read model for recent matches, player stats, history search, queue labels/types, and shared-match stats.
- Optional dashboard Basic auth guard, disabled by default, with health endpoint bypass.
- Low-cardinality Micrometer metrics for poll runs, Riot calls, Telegram sends, outbox enqueue/dispatch, notification backlog, and rate-limit pauses.
- Dashboard/audit rate-limit history and clearer Riot routing copy.
- Docker image and Compose healthchecks.
- CI artifact retention for Surefire reports and packaged jar.
- Skip-safe PostgreSQL Testcontainers coverage and MockMvc E2E fallback without live Riot/Telegram.

## Handoff Items

- Review agent: verify the Testcontainers test is acceptable as skip-safe local coverage and runnable in Docker-enabled CI.
- UI/Ops agent: decide whether browser automation should become a committed build capability.
- Coordinator: ensure CI starts Docker if PostgreSQL Testcontainers coverage is required for release gates.
- Operators: set `APP_DASHBOARD_GUARD_ENABLED=true` plus `APP_DASHBOARD_GUARD_USERNAME` and `APP_DASHBOARD_GUARD_PASSWORD` before exposing the dashboard beyond a trusted LAN.

## Tests Added Or Updated

- Added `src/test/java/com/loltracker/app/match/PostgreSqlPersistenceTest.java`.
- Added `src/test/java/com/loltracker/app/ops/DashboardMvcEndToEndFallbackTest.java`.
- Updated `src/test/java/com/loltracker/app/ops/DashboardControllerTest.java`.
- Added `src/test/java/com/loltracker/app/ops/DashboardAccessGuardFilterTest.java`.
- Added `src/test/java/com/loltracker/app/ops/OpsMetricsTest.java`.
- Updated `src/test/java/com/loltracker/app/integration/riot/RiotClientTest.java`.
- Updated `src/test/resources/application.properties`.
