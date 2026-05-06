# Performance Analysis

## Scope

Assess the cost of richer Telegram notifications.

## Findings

- Derived metrics such as KDA ratio, CS/min, gold/min, damage/min, and vision/min are computed from fields already loaded on the tracked match.
- Shared tracked-player context requires one read-model query by match id.
- New live Riot/API calls during notification dispatch would increase latency and rate-limit exposure.

## Recommendations

- Compute all per-match metrics in memory.
- Fetch shared participants from local DB only.
- Keep Data Dragon/assets, Spectator, mastery, timeline, and challenges out of dispatch until they have caches and bounded refresh policies.

## Risks

- The shared participant query adds a small DB read per notification.
- Aggregated shared notifications remain a larger feature; this round only surfaces shared context in the existing individual notification.
