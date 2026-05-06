# Implementation Plan

## Step 1: Notification Read Model

- Add shared tracked-player stats to `NotificationStatsSnapshot`.
- Query shared participation from `PlayerMatchRepository`.
- Keep fallback behavior when no shared participants are found.

## Step 2: Telegram Formatter

- Redesign `NotificationMessageFactory` output into compact sections.
- Add derived per-minute metrics and KDA ratio.
- Reuse queue labels from `MatchQueueCatalog`.
- Keep HTML escaping for all user/Riot-provided strings.

## Step 3: Tests

- Update existing message formatting tests.
- Add coverage for zero deaths, shared participants, queue labels, and HTML escaping.
- Update notification stats tests for shared-match context.

## Step 4: Documentation And Validation

- Update `06-validation-report.md` with actual commands and outcomes.
- Run focused notification tests.
- Run full tests.
- Run package build.
