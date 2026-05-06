# Architecture Analysis

## Scope

Round 11 addresses Telegram rank reliability and the prioritized product ideas derived from the Riot API review.

## Findings

- Telegram message rendering already has a rank block, but rank data is resolved before formatting.
- `RiotClient.fetchRankEntries` still performs a Summoner-V4 lookup and then calls a League-V4 summoner-id endpoint.
- Current Riot API documentation exposes League-V4 rank lookup by PUUID. This better matches the app identity model.
- Rank refresh failures are intentionally best-effort, but the product currently cannot distinguish unranked from unavailable rank data.
- Outbox compatibility still depends on `tracked_matches`, so rank message work should avoid changing notification identity in this iteration.

## Recommendations

- Make `RiotRankPort` continue to expose rank by PUUID, but update the adapter to call League-V4 `entries/by-puuid` directly.
- Keep rank refresh outside long transactions and preserve fallback to stored snapshots.
- Add product-level rank availability metadata to notification stats.
- Avoid broad schema or notification identity changes unless needed for this bug.

## Risks

- Riot 404 semantics for rank by PUUID may mean wrong platform, not always "unranked".
- Telegram messages can become too noisy if every transient rank failure is printed in full.
- Implementing all P2/P3 ideas fully would exceed a safe single iteration.

## Handoff

- P0 agent: fix Riot rank endpoint and tests.
- P1 agent: improve notification rank status semantics.
- P2 agent: identify low-risk API-backed enrichment candidates.
- P3 agent: document deferred ideas without pretending they are implemented.
