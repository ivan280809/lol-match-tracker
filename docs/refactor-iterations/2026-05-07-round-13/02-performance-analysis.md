# Performance Analysis

## Scope

Rank refresh happens during notification stats creation. This iteration must not add extra Riot calls beyond the existing League-V4 refresh.

## Findings

- The current refresh already fetches all ranked entries for a player in one League-V4 request.
- Persisting both Solo/Duo and Flex snapshots is local DB work and avoids future ambiguity.
- Querying a player's stored rank by queue is a single indexed lookup.

## Recommendation

Reuse the existing Riot League-V4 request, store all supported queue snapshots in one transaction, and add a unique index on `(player_id, queue_type)`.

## Risks

- Notification dispatch still depends on best-effort Riot rank refresh. Existing fallback behavior should remain.

## Handoff

- Avoid new external calls.
- Keep 4xx/429 behavior unchanged in Riot adapter.
- Run focused tests and full Maven validation.
