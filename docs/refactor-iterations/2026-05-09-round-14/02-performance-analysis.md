# Performance Analysis

## Scope

Extra Telegram data must not slow polling noticeably or increase Riot traffic.

## Findings

- Notification stats currently query recent, champion, day, and shared match history.
- New useful views need queue and position history plus a larger recent sample.
- These are bounded reads over indexed columns and can be supported by extra indexes.

## Recommendation

Use fixed windows: recent 30, queue 20, champion 20, position 20. Add indexes on `(player_id, queue_id, game_end_at)`, `(player_id, champion_name, game_end_at)`, `(player_id, lane, game_end_at)`, `(player_id, role, game_end_at)`, and `(player_id, result, game_end_at)`.

## Risks

- The legacy `tracked_matches` table remains in the notification path. A later iteration can move the notification read model to global `matches` + `player_matches`.

## Handoff

- Keep repository methods pageable or top-limited.
- Do not add unbounded scans.
