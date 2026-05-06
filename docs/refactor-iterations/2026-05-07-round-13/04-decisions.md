# Decisions

## Decision 1: Telegram Rank Follows Match Queue

For queue `420`, Telegram will show Solo/Duo rank. For queue `440`, Telegram will show Flex rank. For non-ranked queues, Telegram will continue showing the best available rank using the existing Solo/Duo-first preference.

Reason: the user needs the elo to match the type of ranked game being reported.

Rejected alternative: always show Solo/Duo because it is the most common rank. That is misleading for Flex games.

## Decision 2: Persist Queue-Specific Rank Snapshots

The application will persist rank snapshots per player and Riot queue type in a new `player_rank_snapshots` table.

Reason: a single `players.rank_*` snapshot cannot safely represent both Solo/Duo and Flex.

Rejected alternative: fetch queue-specific rank only in memory and discard it. That would keep fallback behavior weak and lose useful local state.

## Decision 3: Preserve Existing Player Rank Columns

Existing `players.rank_*` columns remain as the primary compatibility snapshot, still selected with Solo/Duo first.

Reason: dashboard sorting, roster average, and existing code depend on those columns.

Rejected alternative: replace every rank read with the new table in this iteration. That widens scope beyond the requested Telegram correction.

## Decision 4: No Additional Riot Calls

The change will reuse the existing League-V4 `entries/by-puuid` request and will not call new Riot endpoints.

Reason: League-V4 already returns all ranked queues for a player in one response.
