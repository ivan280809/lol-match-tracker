# Decisions

## Decision 1: Use League-V4 Rank Lookup By PUUID

`RiotClient.fetchRankEntries` will call `/lol/league/v4/entries/by-puuid/{encryptedPUUID}` directly.

Reason: PUUID is the app's stable identity and Riot's current League-V4 API exposes rank entries by PUUID. This removes a Summoner-V4 dependency and one Riot request per rank refresh.

Rejected alternative: keep Summoner-V4 plus `entries/by-summoner`. It is less reliable, more expensive, and contradicts the current PUUID-first model.

## Decision 2: Keep Rank Refresh Best-Effort But Product-Aware

Rank refresh failures will not block notification delivery, but Telegram/UI-facing text must distinguish unranked/stored/no-data from a refresh error when possible.

Reason: a match notification should still arrive when rank refresh fails, but the user should not be misled into thinking the player has no rank.

Rejected alternative: fail notification delivery when rank refresh fails. That would turn an enrichment failure into a core notification outage.

## Decision 3: Implement P0/P1 Fully, Prepare P2/P3 Honestly

Round 11 will implement the endpoint fix and rank-message clarity. P2/P3 API ideas will be implemented only where they are already low-risk and otherwise documented with priority and required follow-up design.

Reason: Spectator, mastery, challenges, timeline, RSO, and tournament features need new ports, caching, and UX decisions. Implementing all at once would risk destabilizing the distributable build.

Rejected alternative: add all API families immediately. That would increase Riot traffic and product complexity without focused validation.

## Decision 4: Keep Notification Outbox Compatibility

No outbox FK migration is included in this iteration.

Reason: Round 10 intentionally kept outbox compatibility on `tracked_matches`. The rank bug can be fixed without widening persistence scope.

Rejected alternative: migrate outbox to `player_matches` now. It is unrelated to rank correctness.
