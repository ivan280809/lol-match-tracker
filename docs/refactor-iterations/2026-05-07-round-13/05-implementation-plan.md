# Implementation Plan

## Steps

1. Add `player_rank_snapshots` migration with a unique `(player_id, queue_type)` index.
2. Add `PlayerRankEntity` and `PlayerRankRepository`.
3. Extend `PlayerRankSnapshot` with a display queue label.
4. Update `PlayerRankService` to persist Solo/Duo and Flex entries from each refresh.
5. Add queue-specific rank refresh for notification stats.
6. Update Telegram rank text to include the queue label.
7. Add or update unit tests.
8. Run focused notification/player rank tests, then full test and package commands.

## Expected Result

- Solo/Duo ranked notifications show Solo/Duo elo.
- Flex ranked notifications show Flex elo.
- Normal/ARAM/Arena notifications keep showing the best available rank.
- Stored queue-specific rank is used as fallback when Riot refresh fails.
