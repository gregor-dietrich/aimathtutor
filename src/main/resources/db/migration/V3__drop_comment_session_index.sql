-- V3: no query filters comments by session_id any more, so the index V2 created for that
-- only adds a write to every comment insert. The session_id column itself stays.
--
-- IF EXISTS: indexes on adopted databases may have been managed by hand (see V2), so it
-- may already be gone.

DROP INDEX IF EXISTS idx_comment_session_created;
