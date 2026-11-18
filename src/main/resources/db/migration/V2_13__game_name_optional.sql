-- Results and statistics belong to the platform (plan M14), which learns them from MatchEnded.
-- game_results keeps only the first end of a game, so that a continued game does not end twice;
-- the columns and tables the statistics read stay until they are dropped after a backup.
ALTER TABLE game_results ALTER COLUMN game_name DROP NOT NULL;
