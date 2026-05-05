# Cohesion Analysis

## Scope

This document checks where Round 09 behavior belongs in the modular monolith.

## Findings

- `PollingService` currently owns orchestration and can remain the workflow coordinator, but lock acquisition and poll run persistence should stay in `ops`.
- `RiotClient` currently combines Account-V1, Match-V5, Summoner-V4, and League-V4. Ports can expose these responsibilities without splitting deployment.
- `TrackedMatchService` owns legacy match persistence. V2 global match persistence should live in the `match` module and be called from polling.
- `NotificationService` should keep dispatch/outbox responsibility. It should not decide backfill semantics; polling or match persistence should decide whether a saved match should enqueue notification.
- `PlayerService` owns player identity, PUUID resolution, activation/archive, and now `track_from`/backfill onboarding semantics.
- `AppConfigurationService` already owns runtime config source precedence and is the right place for persisted polling config fallback.
- Clock/timezone configuration is cross-cutting and belongs in config, injected into services that produce timestamps or local-day views.

## Recommendations

- Add ports:
  - `RiotAccountPort`
  - `RiotMatchPort`
  - `RiotRankPort`
  - `TelegramNotificationPort`
- Keep adapters in `integration.*`; keep application services depending on ports.
- Add `PollLockService`/repository in `ops`; `PollingService` asks for a lock and updates active run state.
- Add global `MatchEntity`, `PlayerMatchEntity`, and `PlayerMatchService` in `match`.
- Keep `TrackedMatchEntity` and `NotificationOutboxEntity` intact for compatibility, but mark the dual-write path clearly in service naming and docs.
- Add small enums where the domain has finite states: match result, poll status, player sync status, and backfill mode.
- Use product-facing messages from a Riot error mapper rather than surfacing raw exception messages.

## Risks

- The temporary coexistence of `TrackedMatchEntity` and `PlayerMatchEntity` can confuse future readers unless docs and method names are explicit.
- Introducing ports requires test mocks to move from concrete adapters to interfaces in some places.
- Enums stored as `EnumType.STRING` are safe for readability but require care when renaming values.
- Dashboard template can become crowded if polling configuration and active state are added without compact layout.

## Handoff Items

- Keep transaction boundaries short and service-level.
- Keep external adapter exceptions classified at adapter boundary.
- Keep UI copy concise and operational.
- Document any V2 read-model migration left for the next iteration.
