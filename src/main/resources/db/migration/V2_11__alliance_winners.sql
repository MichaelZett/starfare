CREATE TABLE game_result_winners (
    game_id VARCHAR(36) NOT NULL REFERENCES game_results(game_id) ON DELETE CASCADE,
    player_id VARCHAR(30) NOT NULL,
    PRIMARY KEY (game_id, player_id)
);
INSERT INTO game_result_winners (game_id, player_id)
SELECT game_id, winner_player_id FROM game_results WHERE winner_player_id IS NOT NULL;
