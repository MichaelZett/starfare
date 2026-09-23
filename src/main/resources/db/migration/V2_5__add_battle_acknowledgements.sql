-- Welche Schlachten eines Rundenberichts ein Spieler schon ausgewertet hat. Gespeichert
-- wird nur die zuletzt bestätigte Runde je Spieler und Partie; ältere Zeilen entfallen.
CREATE TABLE battle_acknowledgements
(
    player_id       VARCHAR(30) NOT NULL,
    game_id         VARCHAR(36) NOT NULL,
    turn            INTEGER     NOT NULL,
    event_index     INTEGER     NOT NULL,
    acknowledged_at TIMESTAMPTZ NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_battle_acknowledgements PRIMARY KEY (player_id, game_id, turn, event_index)
);
CREATE INDEX idx_battle_acknowledgements_game ON battle_acknowledgements (game_id);
