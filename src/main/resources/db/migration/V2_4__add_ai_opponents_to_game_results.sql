CREATE TABLE game_result_ai_opponents
(
    game_id VARCHAR(36)  NOT NULL,
    ai_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_game_result_ai_opponents PRIMARY KEY (game_id, ai_name),
    CONSTRAINT fk_game_result_ai_opponents_result FOREIGN KEY (game_id) REFERENCES game_results (game_id)
        ON DELETE CASCADE
);
