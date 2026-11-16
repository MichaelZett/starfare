-- Display settings belong to the game, visibility moves to the platform (plan M13): a player may
-- have display settings without a row in user_preferences.
ALTER TABLE user_display_settings DROP CONSTRAINT IF EXISTS user_display_settings_player_id_fkey;
