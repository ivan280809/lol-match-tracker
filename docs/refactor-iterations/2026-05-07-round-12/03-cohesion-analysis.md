# Cohesion Analysis

## Scope

Check whether Telegram enrichment remains cohesive with the modular monolith.

## Findings

- `NotificationMessageFactory` is the right place for presentation formatting.
- `NotificationStatsService` is the right place for local aggregate/read-model facts.
- Queue labels should reuse the existing match queue catalog instead of duplicating hard-coded labels.
- The existing outbox still dispatches individual tracked-match notifications.

## Recommendations

- Make queue catalog access available to notification formatting.
- Add small immutable notification stats records for shared tracked players.
- Do not move outbox identity in this iteration.

## Risks

- Adding formatting helpers can bloat the factory. Keep helpers small and deterministic.
- Shared stats should not trigger lazy-loading surprises; use repository queries that fetch players and matches.
