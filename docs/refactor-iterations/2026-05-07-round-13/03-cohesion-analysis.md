# Cohesion Analysis

## Scope

Keep rank selection in the player domain and Telegram formatting in the notification domain.

## Findings

- Queue classification belongs to the match module.
- Rank queue mapping belongs near `PlayerRankService` because Riot queue type strings are rank-domain details.
- Telegram should receive already formatted product text and should not understand Riot rank selection rules deeply.

## Recommendation

Expose a `refreshRankAvailabilityForMatchQueue` method from `PlayerRankService`. It accepts match queue data and returns a normal `RankRefreshResult`, allowing `NotificationStatsService` to keep its current responsibility.

## Risks

- If rank queue labels are duplicated in UI and notification later, introduce a small catalog.

## Handoff

- Add concise queue labels to the rank snapshot so the message can say `Solo/Duo` or `Flex`.
- Keep old method behavior for other callers.
