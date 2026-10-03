-- V2: indexes the entities used to declare via @Table(indexes = ...) but that the
-- baseline never created. Migrations are now the single source of truth for indexes.
--
-- Skipped, because V1 already has an index on the same table, columns and order:
--   idx_comment_user_created       -> idx_comments_user_created        comments (user_id, created)
--   idx_user_rank                  -> users_rank_id_idx                users (rank_id)
--   idx_user_created               -> users_created_idx                users (created DESC)
--   idx_ai_session                 -> ai_interactions_session_id_idx   ai_interactions (session_id)
--   idx_ai_user                    -> ai_interactions_user_id_idx      ai_interactions (user_id)
--   idx_ai_exercise                -> ai_interactions_exercise_id_idx  ai_interactions (exercise_id)
--   idx_exercise_lesson_published  -> idx_exercise_lesson_published    exercises (lesson_id, published)
--   idx_exercise_public_id         -> idx_exercise_public_id           exercises (public_id)
--   idx_exercise_user_id           -> idx_exercise_user_id             exercises (user_id, created DESC)
--   idx_lesson_parent              -> lessons_parent_id_idx            lessons (parent_id)
--
-- The unique constraints uk_comment_flags_unique and uk_ugm_group_user are already
-- enforced by the unnamed UNIQUE (comment_id, flagger_id) and UNIQUE (user_id, group_id)
-- in V1. idx_ugm_group_user is still created: its column order differs from V1's.

CREATE INDEX idx_session_user_start ON student_sessions (user_id, start_time);
CREATE INDEX idx_session_exercise_start ON student_sessions (exercise_id, start_time);
CREATE INDEX idx_session_completed_start ON student_sessions (completed, start_time);
CREATE INDEX idx_session_start_time ON student_sessions (start_time);

CREATE INDEX idx_ugm_group_user ON user_groups_meta (group_id, user_id);

CREATE INDEX idx_comment_exercise_status_created ON comments (exercise_id, status, created);
CREATE INDEX idx_comment_parent_status_created ON comments (parent_comment_id, status, created);
CREATE INDEX idx_comment_status_created ON comments (status, created);
CREATE INDEX idx_comment_session_created ON comments (session_id, created);
CREATE INDEX idx_comment_flags_status ON comments (flags_count, status);

CREATE INDEX idx_user_activated_banned ON users (activated, banned);
