CREATE TABLE game_results
(
    game_id          VARCHAR(36)  NOT NULL,
    game_name        VARCHAR(200) NOT NULL,
    winner_player_id VARCHAR(30),
    finished_at      TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_game_results PRIMARY KEY (game_id)
);

CREATE TABLE game_result_participants
(
    game_id   VARCHAR(36) NOT NULL,
    player_id VARCHAR(30) NOT NULL,
    CONSTRAINT pk_game_result_participants PRIMARY KEY (game_id, player_id),
    CONSTRAINT fk_game_result_participants_result FOREIGN KEY (game_id) REFERENCES game_results (game_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_game_result_participants_player ON game_result_participants (player_id);
