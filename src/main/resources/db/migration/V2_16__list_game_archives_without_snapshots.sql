-- The platform lists an account's finished games (finishedMatchesOf) from these columns instead of
-- searching and reading every archived snapshot. Starfare fills them when it archives a game; archives
-- stored before (listed = false) are filled once at the start, because places and departures follow
-- the game's rules, which only the application knows.
ALTER TABLE game_archives ADD COLUMN tutorial BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE game_archives ADD COLUMN aborted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE game_archives ADD COLUMN listed BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_game_archives_unlisted ON game_archives (id) WHERE NOT listed;

-- One row per account that held a seat; place and departure only for platform accounts of a ranked game.
CREATE TABLE game_archive_seats
(
    archive_id VARCHAR(36) NOT NULL,
    account    VARCHAR(30) NOT NULL,
    seat       INTEGER     NOT NULL,
    place      INTEGER,
    departure  VARCHAR(20),
    CONSTRAINT pk_game_archive_seats PRIMARY KEY (archive_id, account),
    CONSTRAINT fk_game_archive_seats_archive FOREIGN KEY (archive_id) REFERENCES game_archives (id) ON DELETE CASCADE
);
CREATE INDEX idx_game_archive_seats_account ON game_archive_seats (account, archive_id);
