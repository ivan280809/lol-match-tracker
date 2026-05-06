# Performance Analysis

## Scope

This pass checks the cost of migration, global match reads, observability, and E2E additions.

## Findings

- Global match tables have the right direction, but queries need explicit indexes owned by migrations.
- Backfilling from `tracked_matches` should be idempotent and set-based, not application-loop driven.
- Dashboard lists are bounded, but shared stats need repository queries that avoid loading all history into memory.
- Micrometer metrics should be lightweight counters/timers, not per-match high-cardinality tags.
- Testcontainers can increase local/CI time and should target the DB behavior that H2 cannot prove.

## Recommendations

- Add indexes for:
  - `matches(match_id)`,
  - `matches(game_end_at)`,
  - `player_matches(player_id, match_id)`,
  - `player_matches(player_id, result)`,
  - `player_matches(match_id)`,
  - `tracked_matches(player_id, game_end_at)`,
  - `notification_outbox(status, next_attempt_at)`,
  - `poll_runs(status, started_at)`,
  - `poll_locks(name)`,
  - `players(puuid)` and active/archive filters.
- Use migration SQL to backfill global matches with `select distinct` and participations with conflict-safe inserts.
- Keep dashboard/audit query limits.
- Avoid metrics labels with player names, PUUIDs, or match ids.

## Risks

- PostgreSQL partial indexes differ from H2 behavior.
- CI might not have Docker for Testcontainers unless configured.
- Over-broad shared-stat queries can grow expensive after backfill.

## Handoff Items

- Persistence worker should prefer DB-native set operations for backfill.
- Domain/read-model worker should keep stats queries bounded.
- Quality worker should mark Testcontainers tests skip-safe if Docker is unavailable.
