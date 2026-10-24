-- V3: no query filters comments by session_id any more, so the index V2 created for that
-- only adds a write to every comment insert. The session_id column itself stays.
--
-- IF EXISTS: V2 created it with IF NOT EXISTS, so a database may lack it.

DROP INDEX IF EXISTS idx_comment_session_created;
