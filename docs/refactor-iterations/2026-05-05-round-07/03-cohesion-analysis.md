# Cohesion Analysis

## Scope

This document checks whether responsibilities are placed in the right modules and whether concepts are duplicated or overly implicit.

## Findings

- `PollingService` owns scheduling, player loop orchestration, Riot ingestion, pending notification retry, and notification delivery counting.
- `NotificationService` is synchronous delivery, not a notification application boundary. It has no durable concept of requested delivery.
- `NotificationStatsService` mixes historical DB stats with live Riot rank refresh.
- Domain statuses are strings: match result, poll status, sync status, and notification state.
- `RiotClient` maps low-level HTTP and parsing errors to generic exceptions, so UI and poll state cannot explain the failure well.
- Timezone is hard-coded to `Europe/Madrid` in notification formatting/stats.
- Player identity still allows Riot ID as unique identity. PUUID is stored but nullable and not the primary functional identity.
- The dashboard is a single page with create/toggle/config/run, but no edit/archive/detail/audit workflows.

## Recommendations

- Introduce a notification outbox package as a cohesive application service around requested/sent/failed notification state.
- Keep Telegram formatting in `notification`, but treat Telegram HTTP as an adapter returning a receipt.
- Extract Riot error classification to integration-level exceptions before the UI starts depending on error text.
- Convert statuses to enums incrementally when touching each area.
- Add `Clock` and configurable `ZoneId` after outbox to avoid test and timezone drift.
- Evolve player identity toward PUUID once add/update flows can validate account before persistence without holding transactions.

## Risks

- Creating many small abstractions at once would make the monolith harder to follow.
- UI feature additions need a stronger underlying model first; otherwise dashboard filters/details will duplicate logic.

## Handoff Items

- Domain refactor: add notification status enum or constrained status constants.
- Integration refactor: return Telegram message id when available.
- UI/ops: expose outbox counts now, then build detail/audit pages later.

## Subagent Coordination Notes

`cohesion-analyst` confirmed the outbox direction and identified remaining mixed responsibilities: live rank refresh inside notification stats, string statuses for poll/sync/result, Riot errors as generic exceptions, hard-coded timezone, and Riot ID uniqueness instead of PUUID identity. This round introduces `NotificationDeliveryStatus` and fixes the dashboard `ERROR` color mapping, but broader enum and PUUID work remain pending.
