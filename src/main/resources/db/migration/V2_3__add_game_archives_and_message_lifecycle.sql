CREATE TABLE game_archives
(
    id             VARCHAR(36)  NOT NULL,
    name           VARCHAR(200) NOT NULL,
    host_player_id VARCHAR(30),
    finished_at    TIMESTAMPTZ  NOT NULL,
    state_json     TEXT         NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_game_archives PRIMARY KEY (id)
);
CREATE INDEX idx_game_archives_finished_at ON game_archives (finished_at);
ALTER TABLE direct_messages ADD COLUMN read_at TIMESTAMPTZ;
ALTER TABLE direct_messages ADD COLUMN sender_archived_at TIMESTAMPTZ;
ALTER TABLE direct_messages ADD COLUMN recipient_archived_at TIMESTAMPTZ;
CREATE INDEX idx_direct_messages_sent_at ON direct_messages (sent_at);
