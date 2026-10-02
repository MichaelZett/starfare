ALTER TABLE game_results
    ADD COLUMN ruleset_variant VARCHAR(80) NOT NULL DEFAULT 'classic',
    ADD COLUMN ruleset_version VARCHAR(32) NOT NULL DEFAULT '1.0.0';
