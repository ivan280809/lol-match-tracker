# Architecture Analysis

## Scope

Round 10 targets the remaining V2 release-hardening backlog from Round 09:

- versioned DB migrations,
- legacy history migration to the global match model,
- PostgreSQL/Testcontainers and browser/E2E validation,
- global match read-model adoption,
- minimal operational access control,
- observability and Docker/CI hardening,
- shared-match UX and queue/rate-limit operator improvements.

## Findings

- Round 09 removed reactive HTTP and introduced the global `Match` + `PlayerMatch` model, but production schema ownership still depends on Hibernate `ddl-auto`.
- `tracked_matches` remains the main read model for dashboard stats, player detail, notification stats, and outbox compatibility.
- Existing history created before Round 09 is not guaranteed to exist in `matches` and `player_matches`.
- The project has H2 integration tests and MVC tests but no browser automation infrastructure.
- CI runs Maven tests before publishing the image, but does not retain test reports or the packaged jar as workflow artifacts.
- Docker Compose exposes operational variables but lacks an app healthcheck.
- Security is intentionally out of scope broadly, but a distributable app benefits from an optional access guard when exposed beyond LAN.

## Recommendations

- Adopt Flyway and set production `ddl-auto=validate`.
- Add idempotent migrations for current schema and legacy match backfill.
- Add Testcontainers PostgreSQL tests guarded for Docker availability.
- Add browser/E2E infrastructure if feasible; otherwise add explicit MVC E2E fallback and document the gap.
- Move bounded statistics and player-detail reads to `player_matches` while leaving legacy outbox compatibility intact.
- Add optional dashboard basic auth or token guard disabled by default.
- Add Micrometer counters/timers around poll runs, Riot calls, notification delivery, rate-limit pauses, and outbox backlog.
- Add Docker healthcheck and CI artifact upload.

## Risks

- Moving all notification/outbox reads off `tracked_matches` in one pass is high risk. Keep outbox compatibility until shared notification aggregation is proven.
- Flyway validation can fail existing deployments if the migration baseline does not account for current schemas. Use explicit runbook notes and idempotent SQL where practical.
- Browser tooling may not be present in the local environment. The report must distinguish skipped browser tests from passed browser tests.

## Handoff Items

- `persistence-refactor-agent`: own Flyway, migration SQL, PUUID DB uniqueness, migration tests.
- `domain-refactor-agent`: own enums/read model/queue labels/shared stats.
- `ui-ops-agent`: own dashboard/audit UX, security guard, Docker/CI ops docs.
- `quality-agent`: own Testcontainers, browser/MVC E2E, validation report updates.
- `review-agent`: review transaction boundaries, migration safety, and release readiness.
