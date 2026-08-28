-- Schema der Anwendung (Versionsraum V2_x). Die auth_-Tabellen kommen aus
-- dem Identity-Baustein (de.zettsystems:identity-core, classpath:db/identity,
-- Versionsraum V1_x) und werden hier nicht angelegt.

CREATE SEQUENCE friendship_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE direct_message_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE game_sessions
(
    id            VARCHAR(36)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    host_player_id VARCHAR(30),
    created_at    TIMESTAMPTZ  NOT NULL,
    state_json    TEXT         NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_game_sessions PRIMARY KEY (id)
);

CREATE TABLE friendships
(
    id           BIGINT      NOT NULL DEFAULT nextval('friendship_seq'),
    player_a     VARCHAR(30) NOT NULL,
    player_b     VARCHAR(30) NOT NULL,
    status       VARCHAR(16) NOT NULL,
    requested_by_player_id VARCHAR(30),
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_friendships PRIMARY KEY (id),
    CONSTRAINT uq_friendships_pair UNIQUE (player_a, player_b),
    CONSTRAINT ck_friendships_order CHECK (player_a < player_b),
    CONSTRAINT ck_friendships_status CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED'))
);

CREATE INDEX idx_friendships_player_a ON friendships (player_a);
CREATE INDEX idx_friendships_player_b ON friendships (player_b);

CREATE TABLE user_preferences
(
    player_id  VARCHAR(30) NOT NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'ALL',
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_preferences PRIMARY KEY (player_id),
    CONSTRAINT ck_user_preferences_visibility CHECK (visibility IN ('ALL', 'FRIENDS_ONLY', 'NONE'))
);

CREATE TABLE direct_messages
(
    id                 BIGINT        NOT NULL DEFAULT nextval('direct_message_seq'),
    sender_player_id   VARCHAR(30)   NOT NULL,
    recipient_player_id VARCHAR(30)  NOT NULL,
    text               VARCHAR(1000) NOT NULL,
    sent_at            TIMESTAMPTZ   NOT NULL,
    version            BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_direct_messages PRIMARY KEY (id),
    CONSTRAINT ck_direct_messages_participants CHECK (sender_player_id <> recipient_player_id)
);

CREATE INDEX idx_direct_messages_conversation
    ON direct_messages (sender_player_id, recipient_player_id, sent_at, id);
