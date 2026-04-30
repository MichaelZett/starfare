CREATE TABLE game_sessions (
    id            VARCHAR(36)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    host_username VARCHAR(30),
    created_at    TIMESTAMPTZ  NOT NULL,
    state_json    TEXT         NOT NULL,
    CONSTRAINT pk_game_sessions PRIMARY KEY (id)
);
