# Architecture Analysis

## Scope

Round 08 implements the UI/ops backlog left by round 07:

- player detail, edit, archive, and restore flows,
- dashboard roster filtering and sorting,
- integration health, Telegram test send, Riot validation, and account lookup,
- operational audit views for player errors, notification outbox, poll runs, and external checks.

## Current State

The application is already a Spring MVC + Thymeleaf + JPA monolith. Controllers call application services directly, and there are no internal HTTP calls between modules.

Useful existing boundaries:

- `player`: roster, Riot ID, platform, PUUID, sync status, and rank snapshot.
- `match`: reduced per-player match history.
- `notification`: Telegram message construction and persisted outbox.
- `settings`: encrypted runtime configuration with environment fallback.
- `ops`: dashboard and poll run records.
- `integration.riot` and `integration.telegram`: external HTTP adapters.

## Findings

- The current dashboard renders every player without a shareable filter/sort model.
- `PlayerEntity` has `active`, but no permanent logical archive marker. Toggling active is not enough for "keep history but remove from polling/roster noise".
- Player update always resolves PUUID through Riot. This blocks edits when Riot is not configured and does not distinguish validation failures for operators.
- Rank snapshot fields exist on `PlayerEntity`, but `PlayerView` does not expose them to roster cards.
- The notification outbox exists and can support player detail/audit warnings, but no read view exists yet.
- Runtime configuration can say configured/not configured, but not whether values came from DB or environment fallback.
- Integration checks are transient. There is no persisted "last OK" or external response summary for Riot/Telegram operations.

## Recommendations

- Add `archived_at` to players. Archived players stay queryable and keep match history, but polling uses only active, non-archived players.
- Add small ops read services and view records for dashboard/detail/audit rather than placing filtering/stat aggregation inside Thymeleaf.
- Persist compact external check logs with integration, operation, status, timestamp, and safe summary. Use them for "last OK" and audit history.
- Keep external calls behind `RiotClient` and `TelegramNotifier`. Add classification at adapter/service boundaries, not in controllers.
- Use query params for dashboard filters and sorting. In-memory filtering is acceptable for the current roster size; document DB specifications/indexes as future work.

## Risks

- Adding fields and tables still relies on `spring.jpa.hibernate.ddl-auto=update`; Flyway/Liquibase remains a production hardening gap.
- Account validation without a Riot API key must be explicit to avoid implying a saved player has a verified PUUID.
- Test Telegram can produce real messages in production. The message must be compact and clearly marked as a test.
- External call logs must never store secrets or raw request URLs containing tokens.

## Handoff Items

- `architecture-analyst`: keep flows server-rendered and inside the monolith; no internal HTTP calls.
- `performance-analyst`: keep roster filters bounded and document future DB query/index work.
- `cohesion-analyst`: put archive/account validation in player service, integration tests in ops service, and outbox/audit reads in notification/ops services.
- `ui-ops-agent`: preserve the dense dashboard style while adding detail/edit/audit pages and health actions.
- `quality-agent`: cover service logic, MVC routes, and integration flows with mocked Riot/Telegram adapters.
