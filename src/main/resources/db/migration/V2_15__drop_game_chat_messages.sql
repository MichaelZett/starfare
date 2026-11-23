-- The match chat belongs to the platform, which took over the history in its own migration. Since 0.2.13
-- nothing writes this table and since 0.2.14 no code reads it; the platform backs up the database before
-- this release.
DROP TABLE IF EXISTS game_chat_messages;
DROP SEQUENCE IF EXISTS game_chat_message_seq;
