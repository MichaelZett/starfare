CREATE SEQUENCE friendship_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE friendships
(
    id           BIGINT      NOT NULL DEFAULT nextval('friendship_seq'),
    user_a       VARCHAR(30) NOT NULL,
    user_b       VARCHAR(30) NOT NULL,
    status       VARCHAR(16) NOT NULL,
    requested_by VARCHAR(30),
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_friendships PRIMARY KEY (id),
    CONSTRAINT uq_friendships_pair UNIQUE (user_a, user_b),
    CONSTRAINT ck_friendships_order CHECK (user_a < user_b),
    CONSTRAINT ck_friendships_status CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED'))
);

CREATE INDEX idx_friendships_user_a ON friendships (user_a);
CREATE INDEX idx_friendships_user_b ON friendships (user_b);

CREATE TABLE user_preferences
(
    username   VARCHAR(30) NOT NULL,
    visibility VARCHAR(16) NOT NULL DEFAULT 'ALL',
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_preferences PRIMARY KEY (username),
    CONSTRAINT ck_user_preferences_visibility CHECK (visibility IN ('ALL', 'FRIENDS_ONLY', 'NONE'))
);
