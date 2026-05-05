# Performance Analysis

## Scope

This pass reviews the cost of the requested UI/ops features: dashboard filtering, player detail statistics, integration health, and audit views.

## Findings

- Roster size is expected to remain small. Loading players and filtering in memory is acceptable for this iteration.
- Player detail stats can be computed from the reduced `tracked_matches` table using recent player match queries.
- Champion/text/date filters need match lookups. A simple recent/all match scan is acceptable now, but will not scale indefinitely.
- Notification outbox audit queries should stay bounded and status-filtered.
- Integration health should use persisted compact logs rather than hitting Riot/Telegram on every dashboard load.
- DB health can be a single lightweight query.

## Recommendations

- Bound all UI audit lists: recent matches, recent notifications, recent poll runs, and recent external calls.
- Add repository methods ordered by timestamps instead of lazy iteration from templates.
- Reuse stored rank snapshots on roster cards. Do not refresh rank while rendering pages.
- Persist Riot/Telegram check outcomes only when an operator runs validation/test actions or when future adapters record successful calls.
- Document future indexes for `players.archived_at`, `players.last_polled_at`, `tracked_matches.player_id/game_end_at`, `tracked_matches.champion_name`, `notification_outbox.status/created_at`, and external log timestamps.

## Risks

- Text/champion/date filtering over all matches could become expensive after long retention.
- Without Flyway/Liquibase, new indexes are Hibernate-generated only when supported by ddl-auto behavior.
- DB health checks inside every dashboard request should remain simple to avoid turning the dashboard into an expensive diagnostics endpoint.

## Handoff Items

- Keep dashboard lists small and deterministic.
- Prefer stable sort comparators with ID/name tie breakers.
- Avoid external calls during ordinary GET page renders.
- Use POST actions for Riot validation, account lookup, and Telegram test send.
