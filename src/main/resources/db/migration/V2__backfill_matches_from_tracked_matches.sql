INSERT INTO matches (
    match_id,
    game_mode,
    queue_id,
    duration_seconds,
    game_end_at,
    platform,
    region,
    created_at,
    updated_at
)
SELECT
    legacy.match_id,
    COALESCE(NULLIF(MIN(legacy.game_mode), ''), 'Unknown'),
    MIN(legacy.queue_id),
    MAX(legacy.duration_seconds),
    MAX(legacy.game_end_at),
    COALESCE(NULLIF(MIN(legacy.platform), ''), ''),
    COALESCE(NULLIF(MIN(legacy.region), ''), ''),
    COALESCE(MIN(legacy.created_at), CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP
FROM tracked_matches legacy
WHERE legacy.match_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM matches existing
      WHERE existing.match_id = legacy.match_id
  )
GROUP BY legacy.match_id;

INSERT INTO player_matches (
    player_id,
    match_id,
    puuid,
    champion_id,
    champion_name,
    result,
    lane,
    role,
    kills,
    deaths,
    assists,
    creep_score,
    gold_earned,
    damage_dealt_to_champions,
    vision_score,
    notification_suppressed,
    created_at
)
SELECT
    legacy.player_id,
    global_match.id,
    COALESCE(NULLIF(player.puuid, ''), ''),
    legacy.champion_id,
    COALESCE(NULLIF(legacy.champion_name, ''), 'Unknown'),
    CASE
        WHEN UPPER(legacy.result) IN ('VICTORY', 'DEFEAT', 'UNKNOWN') THEN UPPER(legacy.result)
        ELSE 'UNKNOWN'
    END,
    COALESCE(legacy.lane, ''),
    COALESCE(legacy.role, ''),
    legacy.kills,
    legacy.deaths,
    legacy.assists,
    legacy.creep_score,
    legacy.gold_earned,
    legacy.damage_dealt_to_champions,
    legacy.vision_score,
    legacy.notification_sent,
    COALESCE(legacy.created_at, CURRENT_TIMESTAMP)
FROM tracked_matches legacy
JOIN matches global_match ON global_match.match_id = legacy.match_id
JOIN players player ON player.id = legacy.player_id
WHERE NOT EXISTS (
    SELECT 1
    FROM player_matches existing
    WHERE existing.player_id = legacy.player_id
      AND existing.match_id = global_match.id
);
