-- PostgreSQL initialization script
-- Translated from MariaDB version

BEGIN;

-- --------------------------------------------------------

--
-- Structure for table `exercises`
--

CREATE TABLE exercises (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  user_id BIGINT DEFAULT NULL,
  lesson_id BIGINT DEFAULT NULL,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  commentable BOOLEAN NOT NULL DEFAULT FALSE,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP DEFAULT NULL,
  graspable_enabled BOOLEAN DEFAULT FALSE,
  graspable_initial_expression TEXT,
  graspable_target_expression TEXT,
  graspable_allowed_operations TEXT,
  graspable_difficulty VARCHAR(50),
  graspable_hints TEXT,
  graspable_config TEXT
);

--
-- Relations for table `exercises`:
--   `user_id`
--       `users` -> `id`
--   `lesson_id`
--       `lessons` -> `id`
--

-- Full-text search index for content
CREATE INDEX exercises_content_fts ON exercises USING gin(to_tsvector('english', content));

-- --------------------------------------------------------

--
-- Structure for table `lessons`
--

CREATE TABLE lessons (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  parent_id BIGINT DEFAULT NULL
);

--
-- Relations for table `lessons`:
--   `parent_id`
--       `lessons` -> `id`
--

-- --------------------------------------------------------

--
-- Structure for table `comments`
--

CREATE TABLE comments (
  id BIGSERIAL PRIMARY KEY,
  content TEXT NOT NULL,
  exercise_id BIGINT NOT NULL,
  user_id BIGINT,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--
-- Relations for table `comments`:
--   `user_id`
--       `users` -> `id`
--   `exercise_id`
--       `exercises` -> `id`
--

-- Full-text search index for content
CREATE INDEX comments_content_fts ON comments USING gin(to_tsvector('english', content));

-- --------------------------------------------------------

--
-- Structure for table `user_ranks`
--

CREATE TABLE user_ranks (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  admin_view BOOLEAN NOT NULL DEFAULT FALSE,
  exercise_add BOOLEAN NOT NULL DEFAULT FALSE,
  exercise_delete BOOLEAN NOT NULL DEFAULT FALSE,
  exercise_edit BOOLEAN NOT NULL DEFAULT FALSE,
  lesson_add BOOLEAN NOT NULL DEFAULT FALSE,
  lesson_delete BOOLEAN NOT NULL DEFAULT FALSE,
  lesson_edit BOOLEAN NOT NULL DEFAULT FALSE,
  comment_add BOOLEAN NOT NULL DEFAULT FALSE,
  comment_delete BOOLEAN NOT NULL DEFAULT FALSE,
  comment_edit BOOLEAN NOT NULL DEFAULT FALSE,
  user_add BOOLEAN NOT NULL DEFAULT FALSE,
  user_delete BOOLEAN NOT NULL DEFAULT FALSE,
  user_edit BOOLEAN NOT NULL DEFAULT FALSE,
  user_group_add BOOLEAN NOT NULL DEFAULT FALSE,
  user_group_delete BOOLEAN NOT NULL DEFAULT FALSE,
  user_group_edit BOOLEAN NOT NULL DEFAULT FALSE,
  user_rank_add BOOLEAN NOT NULL DEFAULT FALSE,
  user_rank_delete BOOLEAN NOT NULL DEFAULT FALSE,
  user_rank_edit BOOLEAN NOT NULL DEFAULT FALSE
);

--
-- Relations for table `user_ranks`:
--

--
-- Inserts for table `user_ranks`
--

INSERT INTO user_ranks (id, name, admin_view, exercise_add, exercise_delete, exercise_edit, lesson_add, lesson_delete, lesson_edit, comment_add, comment_delete, comment_edit, user_add, user_delete, user_edit, user_group_add, user_group_delete, user_group_edit, user_rank_add, user_rank_delete, user_rank_edit) VALUES
(1, 'Admin', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),
(2, 'Teacher', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
(3, 'Student', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE);

-- Set sequence to continue from 4
SELECT setval('user_ranks_id_seq', 4);

-- --------------------------------------------------------

--
-- Structure for table `users`
--

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  salt VARCHAR(255) NOT NULL,
  rank_id BIGINT NOT NULL,
  email VARCHAR(255) DEFAULT NULL UNIQUE,
  banned BOOLEAN NOT NULL DEFAULT FALSE,
  activated BOOLEAN NOT NULL DEFAULT FALSE,
  activation_key VARCHAR(255) DEFAULT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  user_avatar_emoji VARCHAR(10) NOT NULL DEFAULT '🧒',
  tutor_avatar_emoji VARCHAR(10) NOT NULL DEFAULT '🤖'
);

--
-- Relations for table `users`:
--   `rank_id`
--       `user_ranks` -> `id`
--

--
-- Inserts for table `users`
--

INSERT INTO users (id, username, password, salt, rank_id, activated) VALUES
(1, 'admin', '3HWqMv8tiSEbBcsUfxqBx7kY4vw+cSvG7OQXp9uzM0w=', '0l/SGC6gqKwYWjw7sm2IrwzIcAjq/QkO9xXcG/LC56c=', 1, TRUE),
(2, 'teacher', 'gqjX9Myv2T0+cSsc7Mk5uP00vWN74acNaV8aVJvvK8Q=', 'Oz3c7v4qJJqqbPHlTzAhilp4O7o+DdW4iBYQMJRABQo=', 2, TRUE),
(3, 'student1', 't/NeeExH/6i3y2DBq77LXyOkGvnk6TCaE1p/lLObE98=', 'tpINgKObPWkbOrylflSrEECZi5ZHvhv2Wjkzlr9HW3E=', 3, TRUE),
(4, 'student2', '0hCDh1yJvbG4VDOqtZWF3qgL3YPUYneknACoEQ6G8Kc=', '4G1YeLz6tsTH98j9zOoEcxvSK0uZnM51uLhF6O6H7pM=', 3, TRUE);

-- Set sequence to continue from 5
SELECT setval('users_id_seq', 4);

-- --------------------------------------------------------

--
-- Structure for table `user_groups`
--

CREATE TABLE user_groups (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL
);

--
-- Inserts for table `user_groups`
--

INSERT INTO user_groups (id, name) VALUES
(1, 'Teacher'),
(2, 'Class 8A'),
(3, 'Class 8B'),
(4, 'Class 9A'),
(5, 'Class 9B');

-- Set sequence to continue from 6
SELECT setval('user_groups_id_seq', 5);

-- --------------------------------------------------------

--
-- Structure for table `user_groups_meta`
--

CREATE TABLE user_groups_meta (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, group_id)
);

--
-- Relations for table `user_groups_meta`:
--   `user_id`
--       `users` -> `id`
--   `group_id`
--       `user_groups` -> `id`
--

--
-- Inserts for table `user_groups_meta`
--

INSERT INTO user_groups_meta (id, user_id, group_id) VALUES
(1, 2, 1),
(2, 3, 4),
(3, 4, 4);

-- Set sequence to continue from 4
SELECT setval('user_groups_meta_id_seq', 3);

-- --------------------------------------------------------

--
-- Structure for table `student_sessions`
--

CREATE TABLE student_sessions (
  id BIGSERIAL PRIMARY KEY,
  session_id VARCHAR(255) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  exercise_id BIGINT NOT NULL,
  start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  end_time TIMESTAMP DEFAULT NULL,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  actions_count INTEGER NOT NULL DEFAULT 0,
  correct_actions INTEGER NOT NULL DEFAULT 0,
  hints_used INTEGER NOT NULL DEFAULT 0,
  final_expression TEXT
);

-- --------------------------------------------------------

--
-- Structure for table `ai_interactions`
--

CREATE TABLE ai_interactions (
  id BIGSERIAL PRIMARY KEY,
  session_id VARCHAR(255) DEFAULT NULL,
  user_id BIGINT DEFAULT NULL,
  exercise_id BIGINT DEFAULT NULL,
  event_type VARCHAR(50) NOT NULL,
  expression_before TEXT,
  expression_after TEXT,
  feedback_type VARCHAR(50) NOT NULL,
  feedback_message TEXT,
  confidence_score DOUBLE PRECISION DEFAULT NULL,
  action_correct BOOLEAN DEFAULT NULL,
  conversation_context TEXT,
  timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------

--
-- Foreign Key Constraints
--

-- Constraints for table `exercises`
ALTER TABLE exercises
  ADD CONSTRAINT exercises_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT exercises_lesson_id_fkey FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- Constraints for table `lessons`
ALTER TABLE lessons
  ADD CONSTRAINT lessons_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES lessons (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- Constraints for table `comments`
ALTER TABLE comments
  ADD CONSTRAINT comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT comments_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE ON UPDATE CASCADE;

-- Constraints for table `users`
ALTER TABLE users
  ADD CONSTRAINT users_rank_id_fkey FOREIGN KEY (rank_id) REFERENCES user_ranks (id) ON UPDATE CASCADE;

-- Constraints for table `user_groups_meta`
ALTER TABLE user_groups_meta
  ADD CONSTRAINT user_groups_meta_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT user_groups_meta_group_id_fkey FOREIGN KEY (group_id) REFERENCES user_groups (id) ON DELETE CASCADE ON UPDATE CASCADE;

-- Constraints for table `student_sessions`
ALTER TABLE student_sessions
  ADD CONSTRAINT student_sessions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT student_sessions_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE ON UPDATE CASCADE;

-- Constraints for table `ai_interactions`
ALTER TABLE ai_interactions
  ADD CONSTRAINT ai_interactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT ai_interactions_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- --------------------------------------------------------

--
-- Additional indexes for foreign keys (PostgreSQL doesn't auto-index FK columns)
--

CREATE INDEX exercises_user_id_idx ON exercises (user_id);
CREATE INDEX exercises_lesson_id_idx ON exercises (lesson_id);
CREATE INDEX lessons_parent_id_idx ON lessons (parent_id);
CREATE INDEX comments_user_id_idx ON comments (user_id);
CREATE INDEX comments_exercise_id_idx ON comments (exercise_id);
CREATE INDEX users_rank_id_idx ON users (rank_id);
CREATE INDEX user_groups_meta_group_id_idx ON user_groups_meta (group_id);
CREATE INDEX student_sessions_user_id_idx ON student_sessions (user_id);
CREATE INDEX student_sessions_exercise_id_idx ON student_sessions (exercise_id);
CREATE INDEX ai_interactions_session_id_idx ON ai_interactions (session_id);
CREATE INDEX ai_interactions_user_id_idx ON ai_interactions (user_id);
CREATE INDEX ai_interactions_exercise_id_idx ON ai_interactions (exercise_id);

COMMIT;
