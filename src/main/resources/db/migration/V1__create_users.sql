CREATE SEQUENCE user_account_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE user_accounts (
    id           BIGINT       NOT NULL DEFAULT nextval('user_account_seq'),
    username     VARCHAR(30)  NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT uq_user_accounts_username UNIQUE (username)
);
