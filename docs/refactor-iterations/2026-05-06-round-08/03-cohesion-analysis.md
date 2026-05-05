# Cohesion Analysis

## Scope

This document checks responsibility placement for the UI/ops feature set.

## Findings

- `DashboardController` already coordinates MVC forms, configuration, player creation, and manual polling.
- `PlayerService` owns duplicate validation, PUUID resolution, active state, and sync state. It is the right place for edit/archive/restore semantics.
- `RiotClient` and `TelegramNotifier` own external HTTP. Controllers should not classify low-level HTTP failures directly.
- `NotificationService` owns outbox lifecycle. It should expose read models for player warnings and audit without leaking entities into templates.
- `AppConfigurationService` owns configuration source knowledge because it sees DB values and environment fallback values.
- The ops package is the right place for integration health/audit orchestration.

## Recommendations

- Add small records for forms and views:
  - dashboard filter parameters,
  - player detail view,
  - recent form/champion stats,
  - outbox warning view,
  - integration health/external check log view.
- Keep controllers thin: parse query/form params, call services, populate model, redirect with flash messages.
- Keep validation messages user-friendly and bounded.
- Keep API controllers compatible with the service changes where possible.

## Risks

- A single controller can grow quickly. If this round makes it hard to navigate, split later into `PlayerPageController` and `OpsPageController` while keeping MVC-only boundaries.
- Adding "status" concepts as strings continues the existing pattern but leaves enum cleanup for later.
- Account validation and update flows share logic; duplicating classification messages would drift.

## Handoff Items

- `domain-refactor-agent`: archive semantics and duplicate/account validation.
- `persistence-refactor-agent`: player archive field and external call log entity/repository.
- `integration-refactor-agent`: friendly Riot/Telegram check result mapping.
- `ui-ops-agent`: dense roster filters, detail/edit pages, health and audit sections.
- `quality-agent`: service/MVC/integration tests for all routes and failure paths.
