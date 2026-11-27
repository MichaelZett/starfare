-- The extended rules were stored under the key "spaceward"; they are called "extended" now. No stored game
-- of the current version 2.0.0 is expected, so this only guards against one; older versions stay
-- unsupported either way. The derived flag "spaceward" in every stored ruleset is read over on load.
UPDATE game_sessions
SET state_json = replace(state_json, '"variant":"spaceward","version":"2.0.0"', '"variant":"extended","version":"2.0.0"')
WHERE state_json LIKE '%"variant":"spaceward","version":"2.0.0"%';

UPDATE game_archives
SET state_json = replace(state_json, '"variant":"spaceward","version":"2.0.0"', '"variant":"extended","version":"2.0.0"')
WHERE state_json LIKE '%"variant":"spaceward","version":"2.0.0"%';
