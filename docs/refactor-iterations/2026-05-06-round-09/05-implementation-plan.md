# Implementation Plan

## Scope

Implement the strongest safe V2 block: reliability, blocking integrations, polling operations, onboarding/backfill semantics, and global match dual-write.

## Steps

1. Integration ports and blocking HTTP:
   - add Riot account/match/rank ports and Telegram notification port,
   - replace `WebClient` clients with `RestClient`,
   - remove WebFlux/Reactor dependencies if no longer used,
   - classify 401/403, 404, 429 with `Retry-After`, region/platform errors, timeout, malformed response, 5xx, and unknown errors.
2. Time configuration:
   - add `Clock` and `ZoneId` beans,
   - inject them into services that currently call `Instant.now()` or hard-code `Europe/Madrid`.
3. Settings:
   - add persisted polling fields and env fallback,
   - extend forms/views/templates for enabled, manual-only, fixed delay, window size, and pagination limit.
4. Polling lock and run state:
   - add poll lock entity/repository/service,
   - add active run fields to poll runs,
   - disable/clarify manual run when lock or rate-limit pause is active,
   - make scheduled polling respect persisted config.
5. Player onboarding:
   - add `track_from` and backfill mode,
   - default `track_from` to clock `now`,
   - import backfill without notification when explicitly enabled,
   - reject duplicate PUUIDs when Riot resolution returns an existing player.
6. Match model:
   - add match result enum, global `MatchEntity`, and `PlayerMatchEntity`,
   - parse reduced Match-V5 data into a richer match detail model,
   - dual-write global match/player match and legacy tracked match in one persistence step,
   - add per-run full-match cache and configurable pagination.
7. Notifications:
   - suppress notifications for imported backfill/old pre-`track_from` matches,
   - keep individual notifications as the compatibility mode,
   - document shared aggregated cards as next migration step if not fully implemented.
8. Tests:
   - update unit tests for blocking ports, polling lock/config, backfill, dedupe, and Clock,
   - add adapter tests for 429, timeout, 401/403, 404, 5xx, malformed response,
   - update MVC dashboard tests for active run, rate-limit pause, disabled manual run, and config form,
   - update integration tests for Match + PlayerMatch dedupe and no live Riot/Telegram.
9. Validation:
   - run `.\mvnw.cmd test`,
   - run `.\mvnw.cmd -DskipTests package`,
   - update `06-validation-report.md` with real results and final readiness review.

## Assumptions

- The current H2 test profile remains the integration test backend.
- Browser automation infrastructure is still absent unless discovered during implementation.
- JPA `ddl-auto` remains the migration mechanism for this iteration; production-safe SQL/Flyway migration is documented as a readiness gap.
- Shared Telegram aggregation is lower priority than preventing duplicate Riot fetches and notification spam.
