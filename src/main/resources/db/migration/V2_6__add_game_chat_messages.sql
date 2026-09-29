CREATE SEQUENCE game_chat_message_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE game_chat_messages
(
    id               BIGINT       NOT NULL,
    game_id          VARCHAR(36)  NOT NULL,
    sender_player_id VARCHAR(30)  NOT NULL,
    text             VARCHAR(1000) NOT NULL,
    sent_at          TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_game_chat_messages PRIMARY KEY (id)
);

CREATE INDEX idx_game_chat_messages_game_sent_at ON game_chat_messages (game_id, sent_at, id);
