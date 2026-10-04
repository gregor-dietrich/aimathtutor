-- V3: stop shipping the admin/admin account that V1 seeds. In production the first
-- admin is now created from app.bootstrap.admin-username/-password on startup.
--
-- The seeded row is only removed while it is provably unused: it still has the
-- published password hash, it is the only account, and nothing but the seeded AI
-- config (ON DELETE SET NULL) references it. That is always true for a new
-- installation. An adopted database whose seeded admin is still in use keeps it;
-- the application then refuses to start in production until the bootstrap
-- password replaces the published one.

DELETE FROM users u
WHERE u.public_id = '01ARZ3NDEKTSV4RRFFQ69G5FB0'
  AND u.password = '$2a$10$oPZWHADXmDcVvg1sf5AZq.UyaigCbI3IcB0TvUDnudPMLhRIOz6yq'
  AND NOT EXISTS (SELECT 1 FROM users o WHERE o.id <> u.id)
  AND NOT EXISTS (SELECT 1 FROM exercises e WHERE e.user_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM comments c WHERE u.id IN (c.user_id, c.deleted_by, c.moderator_id))
  AND NOT EXISTS (SELECT 1 FROM comment_flags f WHERE f.flagger_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM user_groups_meta g WHERE g.user_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM student_sessions s WHERE s.user_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM ai_interactions a WHERE a.user_id = u.id);
