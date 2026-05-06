# Architecture Analysis

## Scope

Telegram rank display currently uses the single rank snapshot stored on `players`. That snapshot is refreshed from Riot League-V4, but the selection prefers Solo/Duo over Flex.

## Findings

- League-V4 returns ranked entries per queue, including `RANKED_SOLO_5x5` and `RANKED_FLEX_SR`.
- Match queue identity is already available through `TrackedMatchEntity.queueId`.
- `MatchQueueCatalog` already classifies `420` as Ranked Solo/Duo and `440` as Ranked Flex.
- The player table only stores one compatibility rank snapshot, which is useful for dashboard sorting but insufficient for queue-specific Telegram messages.

## Recommendation

Add a small persistent rank snapshot model keyed by player and Riot queue type. Keep the existing player rank columns as the primary compatibility snapshot, but resolve Telegram rank from the queue-specific snapshot that matches the match queue.

## Risks

- Existing stored ranks only contain one queue until the next Riot refresh.
- Roster average remains based on the primary stored rank columns in this iteration.

## Handoff

- Persistence: add `player_rank_snapshots` migration, entity, and repository.
- Domain: save all Solo/Flex entries on refresh and select queue-specific rank for notifications.
- Quality: cover Solo/Duo, Flex, non-ranked fallback, and stored queue fallback.
