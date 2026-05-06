# Implementation Plan

## Step 1: Persistence Hardening

- Add Flyway dependency and migration scripts.
- Set production `ddl-auto=validate`.
- Keep test profile explicit.
- Add current schema migration and legacy match backfill migration.
- Add DB-level PUUID uniqueness where safe.
- Add migration validation tests.

## Step 2: Read Model And Domain

- Add bounded repositories/services for global match reads.
- Move player detail/recent stats/shared stats paths toward `player_matches`.
- Add queue labels.
- Add or prepare enums for remaining statuses where safe.
- Keep legacy outbox compatibility.

## Step 3: Ops, Security, Observability

- Add optional dashboard access guard.
- Add low-cardinality metrics around polling, Riot calls, notification dispatch, outbox backlog, and rate-limit pauses.
- Add Docker healthcheck and CI test-report/artifact retention.
- Add audit/dashboard rate-limit history and multi-region operator copy.

## Step 4: Tests And Validation

- Add Testcontainers PostgreSQL coverage for migrations/locks/dedupe when Docker is available.
- Add browser-style E2E if tooling can run; otherwise add MVC E2E fallback and document the gap.
- Expand MVC/UI tests for new ops/security behavior.
- Run:
  - `.\mvnw.cmd test`
  - `.\mvnw.cmd -DskipTests package`

## Step 5: Final Review

- Update `06-validation-report.md` with real command output.
- Classify unresolved items P0/P1/P2/P3.
- Keep final summary honest about skipped environment-dependent tests.
