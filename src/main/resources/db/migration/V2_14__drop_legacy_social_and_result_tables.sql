-- Results, statistics and the social graph belong to the platform (plan M10/M14/M16). Since 0.2.10 no
-- code touches these tables and columns; the platform took over what it needed (platform V2) and backs
-- up the database before this release. game_results keeps only the first end of a game
-- (GameEndEntity: game_id, finished_at, version).
DROP TABLE IF EXISTS game_result_participants;
DROP TABLE IF EXISTS game_result_winners;
DROP TABLE IF EXISTS game_result_ai_opponents;
DROP TABLE IF EXISTS friendships;
DROP TABLE IF EXISTS direct_messages;
DROP TABLE IF EXISTS user_preferences;

ALTER TABLE game_results
    DROP COLUMN IF EXISTS game_name,
    DROP COLUMN IF EXISTS winner_player_id,
    DROP COLUMN IF EXISTS ai_victory,
    DROP COLUMN IF EXISTS ruleset_variant,
    DROP COLUMN IF EXISTS ruleset_version;
