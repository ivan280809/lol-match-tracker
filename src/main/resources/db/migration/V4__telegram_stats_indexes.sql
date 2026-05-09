CREATE INDEX IF NOT EXISTS idx_tracked_matches_player_queue_end
    ON tracked_matches (player_id, queue_id, game_end_at);

CREATE INDEX IF NOT EXISTS idx_tracked_matches_player_champion_end
    ON tracked_matches (player_id, champion_name, game_end_at);

CREATE INDEX IF NOT EXISTS idx_tracked_matches_player_lane_end
    ON tracked_matches (player_id, lane, game_end_at);

CREATE INDEX IF NOT EXISTS idx_tracked_matches_player_role_end
    ON tracked_matches (player_id, role, game_end_at);

CREATE INDEX IF NOT EXISTS idx_tracked_matches_player_result_end
    ON tracked_matches (player_id, result, game_end_at);
