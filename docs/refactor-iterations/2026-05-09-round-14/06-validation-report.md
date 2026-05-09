# Validation Report

## Status

Implemented and locally validated.

## Commands

- `.\mvnw.cmd "-Dtest=NotificationMessageFactoryTest,NotificationStatsServiceTest,FlywayMigrationTest" test`
- `.\mvnw.cmd test`
- `.\mvnw.cmd -DskipTests package`

## Results

- Focused notification/Flyway validation: PASS.
  - 12 tests executed.
  - Covered Telegram rendering, derived notification stats, and Flyway migration loading through H2.
- Full unit/integration/MVC suite: PASS.
  - 205 tests executed.
  - 0 failures.
  - 0 errors.
  - 2 skipped existing Testcontainers/PostgreSQL tests because Docker is not available in the local environment.
- Package: PASS.
  - Built `target/lol-match-tracker-1.0.0.jar`.

## Deploy Readiness Validation

Previous deploy attempt `25465049570` failed at application startup because Hibernate schema validation detected the missing table `player_rank_snapshots`. That failure indicates the target runtime reached JPA validation before the expected Flyway schema was available.

This iteration adds an explicit deploy-time Flyway configuration in `docker-compose.deploy.yml`:

- `SPRING_FLYWAY_ENABLED=true`
- `SPRING_FLYWAY_LOCATIONS=classpath:db/migration`
- `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true`
- `SPRING_FLYWAY_BASELINE_VERSION=0`
- `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`

It also adds migration `V4__telegram_stats_indexes.sql`, so deploys execute all migrations before Hibernate validates the schema. JPA remains in validation mode after migration to avoid silent schema drift.

Remote run `25611460200` validated the application build path successfully:

- CI tests: PASS.
- Package: PASS.
- GHCR image build and push: PASS.
- Deploy: FAILED before application restart while pulling `postgres:15` from Docker Hub due `i/o timeout`.

The deploy workflow now pulls only the application service image before restart. This keeps each release dependent on the freshly built GHCR image and avoids blocking routine app deploys on an unrelated PostgreSQL image refresh.

Remote run `25611573340` validated the hardened deploy path successfully:

- CI tests: PASS.
- Package: PASS.
- GHCR image build and push: PASS.
- Deploy mini PC: PASS.
- Application health check: PASS.

Run URL: `https://github.com/ivan280809/lol-match-tracker/actions/runs/25611573340`.

## Final Distributable Readiness Review

- P0: Ensure the deploy environment always runs Flyway before JPA validation. Addressed by explicit deploy environment variables and confirmed by successful remote application startup.
- P0: Keep production DB migrations complete for existing rank/match tables. Current migration chain passed local validation and remote startup health.
- P0: Avoid Docker Hub pull failures blocking app deploys. Addressed by changing CI deploy to pull only the app image before restart, confirmed by run `25611573340`.
- P1: Add browser/E2E coverage for Telegram configuration and dashboard flows. Existing MVC coverage is present, but no browser E2E infrastructure is available in this repository.
- P1: Add observability around notification composition and skipped derived sections, especially when old rows have partial telemetry.
- P1: Keep hardening Riot rank error visibility so "sin rango" is clearly separated from API failures. Previous iteration fixed the endpoint path; the UI/product distinction can still be improved further.
- P2: Add percentile/record style notification insights once enough match history exists per player.
- P2: Add a migration smoke test against PostgreSQL in CI when Docker/Testcontainers is available.
- P3: Add an operator-facing sample Telegram preview page using stored matches only.

## Implemented Scope

- Added derived Telegram statistics from already stored reduced match data:
  - recent profile,
  - queue profile,
  - champion profile,
  - position profile,
  - delta versus recent baseline,
  - highlight bullets.
- Improved shared-match Telegram output with tracked-player count and group KDA.
- Added repository access paths needed by queue/champion/position profiles.
- Added Flyway indexes for player-scoped historical notification queries.
- Updated deploy compose configuration so Flyway migrations are applied during deployment before Hibernate validation.
- Hardened the deploy workflow so the pull step targets `lol-match-tracker` only.

## Residual Risks

- The local environment cannot execute PostgreSQL/Testcontainers tests because Docker is unavailable.
- Derived Telegram stats depend on the reduced telemetry already persisted; older or partially migrated rows may omit some comparison lines.
- GitHub Actions still warns that Node.js 20 based actions will be deprecated; update actions/runtime settings before GitHub enforces Node.js 24.
