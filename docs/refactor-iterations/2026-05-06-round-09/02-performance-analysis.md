# Performance Analysis

## Scope

This pass reviews the performance risks of robust polling, Riot pagination, shared-match deduplication, and a dual-written match model.

## Findings

- The current poller fetches 10 match ids per player and then fetches every unknown match detail per player.
- If two tracked players share a match, the current flow can fetch and parse the same Riot match detail twice in one poll run.
- `tracked_matches` has a per-player unique constraint, but no global match identity.
- Dashboard query volume is still small, but match-history scans will grow after backfill support.
- Retrying 5xx/timeouts is useful, but retrying 429 immediately makes rate-limit pressure worse.
- Holding DB transactions while waiting on Riot or Telegram increases connection occupancy and can block unrelated UI operations.

## Recommendations

- Fetch match ids in configurable pages and stop when an already-known player match is found or when the safe pagination limit is reached.
- Cache full Riot match details by `matchId` for the duration of a poll run.
- Add a global `matches.match_id` unique constraint and `player_matches(player_id, match_id)` unique constraint for dedupe.
- Save reduced match and participation data only after external data is resolved.
- Store enough reduced stats to avoid re-fetching Riot for UI/statistics: KDA, champion id/name, queue id, lane/role, CS, gold, damage, vision, duration, result, end time, platform, and region.
- Add indexes for poll state, player PUUID, `track_from`, global match end time, and player-match lookup paths.
- Treat 429 as a poll pause using `Retry-After` or a conservative fallback. Do not retry the 429 request immediately.

## Risks

- Backfill can import a lot of old rows. A bounded pagination limit is mandatory until there is a dedicated background backfill workflow.
- Dual-write means reads can temporarily differ between legacy and V2 tables if a save partially fails. The implementation should write both inside the same DB transaction after Riot calls finish.
- Current JPA `ddl-auto=update` may not create all indexes predictably on an existing production schema.
- Browser-level performance remains unmeasured because browser E2E infrastructure does not exist yet.

## Handoff Items

- Keep page size and page count configurable with safe defaults.
- Abort pagination quickly once known history is reached.
- Use the per-run match-detail cache for every player in the run.
- Keep all audit/dashboard lists bounded.
- Record migration/index gaps in the final readiness review.
