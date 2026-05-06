# Performance Analysis

## Scope

Evaluate the rank refresh path and proposed Riot API enrichments for polling and notification cost.

## Findings

- Current rank refresh uses two Riot calls per notification attempt: Summoner-V4 by PUUID, then League-V4 by summoner id.
- Direct League-V4 by PUUID reduces rank refresh to one Riot call.
- Rank refresh can run for queued retry notifications; excessive refresh frequency can increase rate-limit pressure.
- Data Dragon and static constants do not count like live Riot API calls and can be cached aggressively.
- Spectator, mastery, challenges, timeline, and status endpoints add product value but each needs explicit throttling/caching.

## Recommendations

- P0: replace rank refresh with one League-V4 by-PUUID request.
- P1: keep stored rank fallback and make stale/error state visible.
- P2: prefer cached/static enrichment first: queue labels, champion assets, rank icons.
- P2/P3: require TTLs before adding live endpoints such as Spectator, Challenges, Mastery, or Timeline.

## Risks

- Refreshing rank for every notification can still be expensive with many tracked players.
- Timeline endpoints are match-level and can double match detail traffic if used blindly.

## Handoff

- Add tests that assert the direct rank endpoint uses exactly one Riot request.
- Document deferred API features with expected cache/TTL policy.
