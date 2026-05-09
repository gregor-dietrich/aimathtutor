-- PostgreSQL initialization script

SET timezone = 'UTC';

DROP TABLE IF EXISTS ai_config CASCADE;
DROP TABLE IF EXISTS ai_interactions CASCADE;
DROP TABLE IF EXISTS student_sessions CASCADE;
DROP TABLE IF EXISTS user_groups_meta CASCADE;
DROP TABLE IF EXISTS comment_flags CASCADE;
DROP TABLE IF EXISTS comments CASCADE;
DROP TABLE IF EXISTS exercises CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS lessons CASCADE;
DROP TABLE IF EXISTS user_groups CASCADE;
DROP TABLE IF EXISTS user_ranks CASCADE;

-- --------------------------------------------------------

--
-- Structure for table `user_ranks`
--

CREATE TABLE user_ranks (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
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
  user_rank_edit BOOLEAN NOT NULL DEFAULT FALSE,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP
);

CREATE INDEX idx_user_rank_public_id ON user_ranks (public_id);

--
-- Inserts for table `user_ranks`
--

INSERT INTO user_ranks (id, public_id, name, admin_view, exercise_add, exercise_delete, exercise_edit, lesson_add, lesson_delete, lesson_edit, comment_add, comment_delete, comment_edit, user_add, user_delete, user_edit, user_group_add, user_group_delete, user_group_edit, user_rank_add, user_rank_delete, user_rank_edit) VALUES
(1, '01ARZ3NDEKTSV4RRFFQ69G5FAV', 'Admin', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),
(2, '01ARZ3NDEKTSV4RRFFQ69G5FAW', 'Teacher', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
(3, '01ARZ3NDEKTSV4RRFFQ69G5FAX', 'Student', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE);

-- Set sequence to 3 so next value is 4
SELECT setval('user_ranks_id_seq', 3, true);

-- --------------------------------------------------------

--
-- Structure for table `users`
--

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  rank_id BIGINT NOT NULL,
  email VARCHAR(255) DEFAULT NULL UNIQUE,
  banned BOOLEAN NOT NULL DEFAULT FALSE,
  activated BOOLEAN NOT NULL DEFAULT FALSE,
  activation_key VARCHAR(255) DEFAULT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP,
  user_avatar_emoji VARCHAR(10) DEFAULT '🧒',
  tutor_avatar_emoji VARCHAR(10) DEFAULT '🤖'
);

CREATE INDEX idx_user_public_id ON users (public_id);

--
-- Inserts for table `users`
--

INSERT INTO users (id, public_id, username, password, rank_id, activated) VALUES
(1, '01ARZ3NDEKTSV4RRFFQ69G5FB0', 'admin', '$2a$10$oPZWHADXmDcVvg1sf5AZq.UyaigCbI3IcB0TvUDnudPMLhRIOz6yq', 1, TRUE),
(2, '01ARZ3NDEKTSV4RRFFQ69G5FB1', 'teacher', '$2a$10$yvvtRbAoD6FH3wcXZw9QSuc8YSV1CbM/PJMY2lSTrJO2BzbXLC6ly', 2, TRUE),
(3, '01ARZ3NDEKTSV4RRFFQ69G5FB2', 'student1', '$2a$10$oa6TbPoMnJlG/O5kDo.pVerJCfkA1.G0YN/gv2lLAwVQrrBTRK8MC', 3, TRUE),
(4, '01ARZ3NDEKTSV4RRFFQ69G5FB3', 'student2', '$2a$10$i8vt4KcKh/ajw5xGHldP8.lrXX0rrG94S0cJ/XUg.svAajTcZvkeC', 3, TRUE);

-- Set sequence to 4 so next value is 5
SELECT setval('users_id_seq', 4, true);

-- --------------------------------------------------------

--
-- Structure for table `lessons`
--

CREATE TABLE lessons (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(255) NOT NULL,
  parent_id BIGINT DEFAULT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP
);

CREATE INDEX idx_lesson_public_id ON lessons (public_id);

-- --------------------------------------------------------

-- Inserts for table `lessons`

INSERT INTO lessons (id, public_id, name, parent_id) VALUES
(1, '01ARZ3NDEKTSV4RRFFQ69G5FC0', 'Algebra', NULL),
(2, '01ARZ3NDEKTSV4RRFFQ69G5FC1', 'Linear Equations', 1),
(3, '01ARZ3NDEKTSV4RRFFQ69G5FC2', 'Quadratic Equations', 1),
(4, '01ARZ3NDEKTSV4RRFFQ69G5FC3', 'Polynomials', 1);

-- Set sequence to 4 so next value is 5
SELECT setval('lessons_id_seq', 4, true);

-- --------------------------------------------------------

-- --------------------------------------------------------

--
-- Structure for table `exercises`
--

CREATE TABLE exercises (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  user_id BIGINT DEFAULT NULL,
  lesson_id BIGINT DEFAULT NULL,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  commentable BOOLEAN NOT NULL DEFAULT FALSE,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP DEFAULT NULL,
  graspable_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  graspable_initial_expression TEXT,
  graspable_target_expression TEXT,
  graspable_difficulty VARCHAR(50),
  graspable_hints TEXT
);

-- Full-text search index for content
CREATE INDEX exercises_content_fts ON exercises USING gin(to_tsvector('english', content));

CREATE INDEX idx_exercise_public_id ON exercises (public_id);
CREATE INDEX idx_exercise_lesson_published ON exercises (lesson_id, published);
CREATE INDEX idx_exercise_user_id ON exercises (user_id, created DESC);

-- --------------------------------------------------------

-- Seed exercises for lessons

INSERT INTO exercises (id, public_id, title, content, user_id, lesson_id, published, commentable, graspable_enabled, graspable_initial_expression, graspable_target_expression, graspable_difficulty, graspable_hints)
VALUES
  (1, '01ARZ3NDEKTSV4RRFFQ69G5FD0', 'Solve for x: simple linear', 'Solve the equation for x: 2x + 3 = 11', 2, 2, TRUE, TRUE, TRUE, '2*x + 3 = 11', 'x = 4', 'BEGINNER', '["Isolate the term with x","Subtract 3 from both sides","Divide both sides by 2"]'),
  (2, '01ARZ3NDEKTSV4RRFFQ69G5FD1', 'Two-step linear equation', 'Solve: 3(x - 2) = 9', 2, 2, TRUE, TRUE, TRUE, '3*(x - 2) = 9', 'x = 5', 'BEGINNER', '["Divide both sides by 3","Then add 2 to both sides"]'),
  (3, '01ARZ3NDEKTSV4RRFFQ69G5FD2', 'Linear equation with fractions', 'Solve: (1/2)x + 1 = 4', 2, 2, TRUE, TRUE, TRUE, '(1/2)*x + 1 = 4', 'x = 6', 'INTERMEDIATE', '["Eliminate fractions by multiplying both sides","Isolate x"]'),
  (4, '01ARZ3NDEKTSV4RRFFQ69G5FD3', 'Expand and simplify', 'Expand and simplify the expression (x + 2)(x - 3).', 2, 4, TRUE, TRUE, TRUE, '(x + 2)*(x - 3)', 'x^2 - x - 6', 'INTERMEDIATE', '["Use distributive property","Combine like terms"]'),
  (5, '01ARZ3NDEKTSV4RRFFQ69G5FD4', 'Solve quadratic by factoring', 'Solve for x by factoring: x^2 - 5x + 6 = 0', 2, 3, TRUE, TRUE, TRUE, 'x^2 - 5*x + 6 = 0', 'x = 2 or x = 3', 'INTERMEDIATE', '["Find two numbers that multiply to 6 and add to -5","Set each factor to zero"]'),
  (6, '01ARZ3NDEKTSV4RRFFQ69G5FD5', 'Complete the square', 'Solve by completing the square: x^2 + 6x + 5 = 0', 2, 3, TRUE, TRUE, TRUE, 'x^2 + 6*x + 5 = 0', 'x = -1 or x = -5', 'ADVANCED', '["Move constant to the right","Add (b/2)^2 to both sides","Take square root of both sides"]'),
  (7, '01ARZ3NDEKTSV4RRFFQ69G5FD6', 'Quadratic formula', 'Use the quadratic formula to solve: 2x^2 - 4x - 6 = 0', 2, 3, TRUE, TRUE, TRUE, '2*x^2 - 4*x - 6 = 0', 'x = 2 or x = -1.5', 'ADVANCED', '["Identify a, b, c","Apply the quadratic formula","Simplify the results"]');

INSERT INTO exercises (id, public_id, title, content, user_id, lesson_id, published, commentable, graspable_enabled)
VALUES
  (8, '01ARZ3NDEKTSV4RRFFQ69G5FD7', 'Standalone Exercise', 'This exercise is not in any category and does not have Graspable Math enabled. Just for testing.', 2, NULL, TRUE, TRUE, FALSE);

-- Set sequence to 8 so next value is 9
SELECT setval('exercises_id_seq', 8, true);


--
-- Structure for table `comments`
--

CREATE TABLE comments (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  content TEXT NOT NULL,
  exercise_id BIGINT NOT NULL,
  user_id BIGINT,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  parent_comment_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  flags_count INT NOT NULL DEFAULT 0,
  session_id VARCHAR(255),
  last_edit TIMESTAMP,
  deleted_by BIGINT,
  deleted_at TIMESTAMP,
  moderation_reason VARCHAR(500),
  moderator_id BIGINT,
  moderation_action VARCHAR(20),
  moderated_at TIMESTAMP
);

-- Performance indexes
CREATE INDEX idx_comments_exercise_id ON comments(exercise_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_deleted_by ON comments(deleted_by);
CREATE INDEX idx_comments_moderator_id ON comments(moderator_id);
CREATE INDEX idx_comments_session_id ON comments(session_id);
CREATE INDEX idx_comments_created ON comments(created);
CREATE INDEX idx_comments_status ON comments(status);
CREATE INDEX idx_comments_user_created ON comments(user_id, created);
CREATE INDEX idx_comment_public_id ON comments (public_id);

-- Full-text search index for content
CREATE INDEX comments_content_fts ON comments USING gin(to_tsvector('english', content));

-- Table to track which users have flagged which comments
CREATE TABLE comment_flags (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  comment_id BIGINT NOT NULL,
  flagger_id BIGINT NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP,
  UNIQUE(comment_id, flagger_id),
  FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
  FOREIGN KEY (flagger_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_comment_flags_comment_id ON comment_flags(comment_id);
CREATE INDEX idx_comment_flags_flagger_id ON comment_flags(flagger_id);
CREATE INDEX idx_comment_flag_public_id ON comment_flags (public_id);

-- --------------------------------------------------------

--
-- Structure for table `user_groups`
--

CREATE TABLE user_groups (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(255) NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP
);

CREATE INDEX idx_user_group_public_id ON user_groups (public_id);

--
-- Inserts for table `user_groups`
--

INSERT INTO user_groups (id, public_id, name) VALUES
(1, '01ARZ3NDEKTSV4RRFFQ69G5FE0', 'Teacher'),
(2, '01ARZ3NDEKTSV4RRFFQ69G5FE1', 'Class 8A'),
(3, '01ARZ3NDEKTSV4RRFFQ69G5FE2', 'Class 8B'),
(4, '01ARZ3NDEKTSV4RRFFQ69G5FE3', 'Class 9A'),
(5, '01ARZ3NDEKTSV4RRFFQ69G5FE4', 'Class 9B');

-- Set sequence to 5 so next value is 6
SELECT setval('user_groups_id_seq', 5, true);

-- --------------------------------------------------------

--
-- Structure for table `user_groups_meta`
--

CREATE TABLE user_groups_meta (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP,
  UNIQUE (user_id, group_id)
);

-- Performance indexes
CREATE INDEX user_groups_meta_user_id_idx ON user_groups_meta (user_id);
CREATE INDEX user_groups_meta_group_id_idx ON user_groups_meta (group_id);
CREATE INDEX idx_user_group_meta_public_id ON user_groups_meta (public_id);

--
-- Inserts for table `user_groups_meta`
--

INSERT INTO user_groups_meta (id, public_id, user_id, group_id) VALUES
(1, '01ARZ3NDEKTSV4RRFFQ69G5FF0', 2, 1),
(2, '01ARZ3NDEKTSV4RRFFQ69G5FF1', 3, 4),
(3, '01ARZ3NDEKTSV4RRFFQ69G5FF2', 4, 4);

-- Set sequence to 3 so next value is 4
SELECT setval('user_groups_meta_id_seq', 3, true);

-- --------------------------------------------------------

--
-- Structure for table `student_sessions`
--

CREATE TABLE student_sessions (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  session_id VARCHAR(255) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  exercise_id BIGINT NOT NULL,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  actions_count INTEGER NOT NULL DEFAULT 0,
  correct_actions INTEGER NOT NULL DEFAULT 0,
  hints_used INTEGER NOT NULL DEFAULT 0,
  final_expression TEXT,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP
);

-- Performance indexes
CREATE INDEX student_sessions_user_id_idx ON student_sessions (user_id);
CREATE INDEX student_sessions_exercise_id_idx ON student_sessions (exercise_id);
CREATE INDEX idx_student_session_public_id ON student_sessions (public_id);

-- --------------------------------------------------------

--
-- Structure for table `ai_interactions`
--

CREATE TABLE ai_interactions (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  session_id VARCHAR(255) DEFAULT NULL,
  user_id BIGINT DEFAULT NULL,
  exercise_id BIGINT DEFAULT NULL,
  event_type VARCHAR(50) NOT NULL,
  student_message TEXT,
  expression_before TEXT,
  expression_after TEXT,
  feedback_type VARCHAR(50) NOT NULL,
  feedback_message TEXT,
  confidence_score DOUBLE PRECISION DEFAULT NULL,
  action_correct BOOLEAN NOT NULL DEFAULT NULL,
  conversation_context TEXT,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP
);

-- Performance indexes
CREATE INDEX ai_interactions_session_id_idx ON ai_interactions (session_id);
CREATE INDEX ai_interactions_user_id_idx ON ai_interactions (user_id);
CREATE INDEX ai_interactions_exercise_id_idx ON ai_interactions (exercise_id);
CREATE INDEX idx_ai_interaction_public_id ON ai_interactions (public_id);

-- --------------------------------------------------------

--
-- Structure for table `ai_config`
--

CREATE TABLE ai_config (
  id BIGSERIAL PRIMARY KEY,
  public_id VARCHAR(26) NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  config_key VARCHAR(255) NOT NULL UNIQUE,
  config_value TEXT,
  config_type VARCHAR(50),
  is_optional BOOLEAN NOT NULL DEFAULT false,
  category VARCHAR(50) NOT NULL,
  description TEXT,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_edit TIMESTAMP,
  last_updated_by BIGINT DEFAULT NULL,
  CONSTRAINT fk_ai_config_user FOREIGN KEY (last_updated_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_ai_config_public_id ON ai_config (public_id);

-- Seed AI configuration with defaults from application.properties and hardcoded prompts
INSERT INTO ai_config (id, public_id, config_key, config_value, config_type, category, description, is_optional, last_updated_by) VALUES
-- General settings
(1, '01ARZ3NDEKTSV4RRFFQ69G5FG0', 'ai.tutor.enabled', 'true', 'BOOLEAN', 'GENERAL', 'Enable or disable AI tutor functionality', false, 1),
(2, '01ARZ3NDEKTSV4RRFFQ69G5FG1', 'ai.tutor.provider', 'mock', 'STRING', 'GENERAL', 'AI provider to use: mock, gemini, openai, or ollama', false, 1),

-- Gemini settings
(3, '01ARZ3NDEKTSV4RRFFQ69G5FG2', 'gemini.model', 'gemma-4-31b-it', 'STRING', 'GEMINI', 'Gemini model name', false, 1),
(4, '01ARZ3NDEKTSV4RRFFQ69G5FG3', 'gemini.api.base-url', 'https://generativelanguage.googleapis.com', 'STRING', 'GEMINI', 'Gemini API base URL', false, 1),
(5, '01ARZ3NDEKTSV4RRFFQ69G5FG4', 'gemini.temperature', '0.7', 'DOUBLE', 'GEMINI', 'Gemini temperature setting (0.0-2.0)', false, 1),
(6, '01ARZ3NDEKTSV4RRFFQ69G5FG5', 'gemini.max-tokens', '2000', 'INTEGER', 'GEMINI', 'Gemini maximum tokens for responses', false, 1),

-- OpenAI settings
(7, '01ARZ3NDEKTSV4RRFFQ69G5FG6', 'openai.model', 'gpt-5-nano', 'STRING', 'OPENAI', 'OpenAI model name', false, 1),
(8, '01ARZ3NDEKTSV4RRFFQ69G5FG7', 'openai.organization-id', '', 'STRING', 'OPENAI', 'OpenAI organization ID (optional)', true, 1),
(9, '01ARZ3NDEKTSV4RRFFQ69G5FG8', 'openai.api.base-url', 'https://api.openai.com/v1', 'STRING', 'OPENAI', 'OpenAI API base URL', false, 1),
(10, '01ARZ3NDEKTSV4RRFFQ69G5FG9', 'openai.temperature', '0.7', 'DOUBLE', 'OPENAI', 'OpenAI temperature setting (0.0-2.0)', false, 1),
(11, '01ARZ3NDEKTSV4RRFFQ69G5FGA', 'openai.max-tokens', '2000', 'INTEGER', 'OPENAI', 'OpenAI maximum tokens for responses', false, 1),

-- Ollama settings
(12, '01ARZ3NDEKTSV4RRFFQ69G5FGB', 'ollama.api.url', 'http://ollama:11434', 'STRING', 'OLLAMA', 'Ollama API URL', false, 1),
(13, '01ARZ3NDEKTSV4RRFFQ69G5FGC', 'ollama.model', 'llama3.2:3b', 'STRING', 'OLLAMA', 'Ollama model name', false, 1),
(14, '01ARZ3NDEKTSV4RRFFQ69G5FGD', 'ollama.temperature', '0.7', 'DOUBLE', 'OLLAMA', 'Ollama temperature setting (0.0-2.0)', false, 1),
(15, '01ARZ3NDEKTSV4RRFFQ69G5FGE', 'ollama.max-tokens', '2000', 'INTEGER', 'OLLAMA', 'Ollama maximum tokens for responses', false, 1),
(16, '01ARZ3NDEKTSV4RRFFQ69G5FGF', 'ollama.timeout-seconds', '30', 'INTEGER', 'OLLAMA', 'Ollama API timeout in seconds', false, 1),

-- Prompt settings
(17, '01ARZ3NDEKTSV4RRFFQ69G5FGG', 'ai.prompt.question.answering.prefix', 'You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question.', 'TEXT', 'PROMPTS', 'Prefix prompt for question answering', false, 1),
(18, '01ARZ3NDEKTSV4RRFFQ69G5FGH', 'ai.prompt.question.answering.postfix', 'Provide a helpful, encouraging answer that:
- Guides the student''s thinking without solving it for them
- Is concise (2-3 sentences max)
- Relates to their current problem if possible
- Uses clear, simple language
- Encourages them to try the next step

Your answer:', 'TEXT', 'PROMPTS', 'Postfix prompt for question answering', false, 1),
(19, '01ARZ3NDEKTSV4RRFFQ69G5FGJ', 'ai.prompt.math.tutoring.prefix', 'You are an encouraging but concise AI math tutor helping a student learn algebra. Analyze the student''s action and provide brief, helpful feedback.', 'TEXT', 'PROMPTS', 'Prefix prompt for math tutoring', false, 1),
(20, '01ARZ3NDEKTSV4RRFFQ69G5FGK', 'ai.prompt.math.tutoring.postfix', 'Provide feedback in the following JSON format:
{
  "type": "POSITIVE" or "CORRECTIVE" or "HINT" or "SUGGESTION",
  "message": "Your brief, encouraging feedback (ONE sentence only)",
  "hints": [],
  "suggestedNextSteps": [],
  "confidence": 0.0 to 1.0
}

IMPORTANT Guidelines:
- Keep message to ONE SHORT sentence (max 15 words)
- Be encouraging but not overly enthusiastic
- If the action is correct, give brief praise
- If incorrect, point out the error gently
- Only provide hints array if student made a mistake (max 1-2 hints)
- Do NOT provide hints for correct actions
- Leave suggestedNextSteps empty unless specifically needed
- Be specific about what they did, not generic', 'TEXT', 'PROMPTS', 'Postfix prompt for math tutoring', false, 1);

-- Set sequence to 20 so next value is 21
SELECT setval('ai_config_id_seq', 20, true);

-- Performance indexes
CREATE INDEX ai_config_key_idx ON ai_config (config_key);
CREATE INDEX ai_config_category_idx ON ai_config (category);

-- --------------------------------------------------------

--
-- Foreign Key Constraints
--

-- Constraints for table `lessons`
ALTER TABLE lessons
  ADD CONSTRAINT lessons_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES lessons (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- Constraints for table `exercises`
ALTER TABLE exercises
  ADD CONSTRAINT exercises_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT exercises_lesson_id_fkey FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- Constraints for table `comments`
ALTER TABLE comments
  ADD CONSTRAINT fk_comments_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments (id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_comments_deleted_by FOREIGN KEY (deleted_by) REFERENCES users (id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_comments_moderator FOREIGN KEY (moderator_id) REFERENCES users (id) ON DELETE SET NULL;

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

--
-- Seed data for `student_sessions`
--

INSERT INTO student_sessions (id, public_id, session_id, user_id, exercise_id, start_time, end_time, completed, actions_count, correct_actions, hints_used) VALUES
  (1, '01ARZ3NDEKTSV4RRFFQ69G5F00', 'a8720aac-f666-420f-8ddd-9253f9248d94', 3, 5, '2026-05-09 20:34:56', '2026-05-09 21:02:56', TRUE, 28, 14, 2),
  (2, '01ARZ3NDEKTSV4RRFFQ69G5F01', 'ac0b4cbb-08bc-48d8-9064-367235ca7df9', 3, 2, '2026-05-09 20:20:56', '2026-05-09 21:02:56', TRUE, 24, 12, 0),
  (3, '01ARZ3NDEKTSV4RRFFQ69G5F02', 'b119a065-951c-484b-9b3d-cc09d02e0459', 3, 3, '2026-05-08 08:07:56', '2026-05-08 08:25:56', TRUE, 32, 16, 4),
  (4, '01ARZ3NDEKTSV4RRFFQ69G5F03', '198f502b-3853-4302-84e5-f003610e0bc7', 3, 3, '2026-05-08 17:08:56', '2026-05-08 17:31:56', FALSE, 7, 4, 0),
  (5, '01ARZ3NDEKTSV4RRFFQ69G5F04', 'd60d80ae-193f-453b-9c57-7d0f9e67956f', 3, 5, '2026-05-07 19:35:56', '2026-05-07 19:59:56', TRUE, 34, 20, 0),
  (6, '01ARZ3NDEKTSV4RRFFQ69G5F05', 'f7d38d78-6ac2-4fcc-964d-337986710ea2', 3, 1, '2026-05-07 03:09:56', '2026-05-07 03:24:56', FALSE, 12, 8, 1),
  (7, '01ARZ3NDEKTSV4RRFFQ69G5F06', '88dffd58-6048-4bcd-afae-bbedf9d2590d', 3, 6, '2026-05-06 09:04:56', '2026-05-06 09:46:56', TRUE, 10, 6, 3),
  (8, '01ARZ3NDEKTSV4RRFFQ69G5F07', '4fb47f0b-c745-40ad-9a6e-9e87a85b75c7', 3, 4, '2026-05-06 18:43:56', '2026-05-06 19:01:56', TRUE, 12, 6, 4),
  (9, '01ARZ3NDEKTSV4RRFFQ69G5F08', '007cb42a-92b1-4f4f-bde9-068df410ae12', 3, 7, '2026-05-06 00:37:56', '2026-05-06 00:58:56', TRUE, 31, 22, 6),
  (10, '01ARZ3NDEKTSV4RRFFQ69G5F09', '8a404e07-cb5d-4e65-a4cb-1232a8485657', 3, 6, '2026-05-05 19:08:56', '2026-05-05 19:40:56', FALSE, 27, 16, 4),
  (11, '01ARZ3NDEKTSV4RRFFQ69G5F0A', 'f13421c0-32bb-46e8-9b16-d8ed55075910', 3, 5, '2026-05-04 00:52:56', '2026-05-04 01:18:56', TRUE, 28, 18, 2),
  (12, '01ARZ3NDEKTSV4RRFFQ69G5F0B', 'a967e820-8e14-4b60-b089-db1116a2c948', 3, 6, '2026-05-03 16:49:56', '2026-05-03 17:00:56', TRUE, 32, 17, 5),
  (13, '01ARZ3NDEKTSV4RRFFQ69G5F0C', '1c4ad1f4-b469-445b-91dc-6d00983c7810', 3, 1, '2026-05-02 07:43:56', '2026-05-02 08:27:56', TRUE, 19, 12, 6),
  (14, '01ARZ3NDEKTSV4RRFFQ69G5F0D', '78154d6d-979e-43a0-a34b-789628035bf0', 3, 3, '2026-05-01 22:14:56', '2026-05-01 22:22:56', TRUE, 13, 9, 2),
  (15, '01ARZ3NDEKTSV4RRFFQ69G5F0E', 'aa76f7a5-3b1f-40a1-965c-31276748bea8', 3, 1, '2026-05-01 15:52:56', '2026-05-01 16:33:56', TRUE, 28, 16, 1),
  (16, '01ARZ3NDEKTSV4RRFFQ69G5F0F', '76f46177-be8d-430b-8a71-32508271b015', 3, 7, '2026-04-30 00:49:56', '2026-04-30 01:23:56', TRUE, 29, 15, 2),
  (17, '01ARZ3NDEKTSV4RRFFQ69G5F0G', '731850d9-0bd6-4780-9d9b-3838f440b349', 3, 1, '2026-04-29 02:01:56', '2026-04-29 02:17:56', TRUE, 16, 12, 3),
  (18, '01ARZ3NDEKTSV4RRFFQ69G5F0H', 'd48bfdfb-f82d-4b0a-a00f-2031720578ec', 3, 5, '2026-04-29 03:16:56', '2026-04-29 03:25:56', TRUE, 7, 5, 1),
  (19, '01ARZ3NDEKTSV4RRFFQ69G5F0J', 'ff40802b-ba29-485b-9eef-ab04fe4451d9', 3, 5, '2026-04-29 16:39:56', '2026-04-29 17:24:56', TRUE, 13, 8, 0),
  (20, '01ARZ3NDEKTSV4RRFFQ69G5F0K', 'dbff5b76-76cc-4545-b979-ac352890c0f4', 3, 4, '2026-04-28 11:56:56', '2026-04-28 12:04:56', FALSE, 19, 14, 0),
  (21, '01ARZ3NDEKTSV4RRFFQ69G5F0M', 'c7f9c58a-57d3-46e7-a47b-176377cc9111', 3, 2, '2026-04-27 19:00:56', '2026-04-27 19:05:56', TRUE, 23, 13, 5),
  (22, '01ARZ3NDEKTSV4RRFFQ69G5F0N', 'a0fb369a-5fe1-43a8-81bd-38a8856910b2', 3, 2, '2026-04-26 11:17:56', '2026-04-26 11:54:56', TRUE, 20, 11, 4),
  (23, '01ARZ3NDEKTSV4RRFFQ69G5F0P', 'c87572de-09b7-44b9-9061-f11732bd78dd', 3, 4, '2026-04-26 02:51:56', '2026-04-26 03:18:56', TRUE, 18, 10, 4),
  (24, '01ARZ3NDEKTSV4RRFFQ69G5F0Q', 'e9947669-da86-4a90-af9c-6535e38f1d44', 3, 4, '2026-04-26 06:26:56', '2026-04-26 06:48:56', TRUE, 25, 13, 4),
  (25, '01ARZ3NDEKTSV4RRFFQ69G5F0R', '957bf2a5-2dd9-4120-bc59-b5683d085e40', 3, 1, '2026-04-25 18:06:56', '2026-04-25 18:35:56', TRUE, 9, 5, 2),
  (26, '01ARZ3NDEKTSV4RRFFQ69G5F0S', '7895b08d-5796-4e02-ab9d-4124a149e017', 3, 1, '2026-04-25 04:15:56', '2026-04-25 04:31:56', TRUE, 31, 15, 4),
  (27, '01ARZ3NDEKTSV4RRFFQ69G5F0T', '814046a0-c8f9-40d4-bca0-9068ddd5c338', 3, 7, '2026-04-24 05:51:56', '2026-04-24 06:21:56', TRUE, 6, 3, 4),
  (28, '01ARZ3NDEKTSV4RRFFQ69G5F0V', '9aeeb8cc-309f-4c00-8d7c-54c85b902fd0', 3, 2, '2026-04-22 22:35:56', '2026-04-22 22:54:56', FALSE, 27, 17, 3),
  (29, '01ARZ3NDEKTSV4RRFFQ69G5F0W', '2fcaffc4-cb3b-4638-820b-b611ced15db3', 3, 7, '2026-04-22 19:44:56', '2026-04-22 20:18:56', FALSE, 15, 7, 0),
  (30, '01ARZ3NDEKTSV4RRFFQ69G5F0X', 'ec4a28ee-6a5c-428b-9d4c-1f8bff328bb3', 3, 2, '2026-04-21 05:16:56', '2026-04-21 05:42:56', FALSE, 7, 4, 1),
  (31, '01ARZ3NDEKTSV4RRFFQ69G5F0Y', 'fd608918-f167-4b9f-aaba-72afee9e8c37', 3, 3, '2026-04-20 03:06:56', '2026-04-20 03:20:56', TRUE, 7, 4, 0),
  (32, '01ARZ3NDEKTSV4RRFFQ69G5F0Z', '3484c176-cf8a-4db6-b019-905ce166d69a', 3, 3, '2026-04-19 11:06:56', '2026-04-19 11:11:56', TRUE, 25, 14, 1),
  (33, '01ARZ3NDEKTSV4RRFFQ69G5F10', '9e4740f5-7874-4ab9-a41c-a8490617ff31', 3, 3, '2026-04-19 20:52:56', '2026-04-19 20:57:56', TRUE, 8, 4, 3),
  (34, '01ARZ3NDEKTSV4RRFFQ69G5F11', 'f66f7ee8-8970-4f40-ba58-6082ee766563', 3, 7, '2026-04-18 10:03:56', '2026-04-18 10:45:56', TRUE, 27, 15, 4),
  (35, '01ARZ3NDEKTSV4RRFFQ69G5F12', '18905319-ec0a-416a-8bcd-17340e9ccf3e', 3, 1, '2026-04-18 05:21:56', '2026-04-18 05:28:56', TRUE, 34, 23, 3),
  (36, '01ARZ3NDEKTSV4RRFFQ69G5F13', 'cb58b746-e466-49df-aff1-4bbd8e7300a8', 3, 2, '2026-04-17 21:46:56', '2026-04-17 22:30:56', TRUE, 11, 7, 2),
  (37, '01ARZ3NDEKTSV4RRFFQ69G5F14', 'a00790c2-2a83-4eb0-938a-0460a265663c', 3, 1, '2026-04-17 12:49:56', '2026-04-17 12:50:56', TRUE, 32, 16, 0),
  (38, '01ARZ3NDEKTSV4RRFFQ69G5F15', '86ffd908-f6a4-4842-a27f-c8550279fbf6', 3, 5, '2026-04-17 10:32:56', '2026-04-17 11:00:56', TRUE, 10, 7, 0),
  (39, '01ARZ3NDEKTSV4RRFFQ69G5F16', '37ede4b1-9284-4a80-b362-50cb5fcad2cb', 3, 2, '2026-04-16 18:17:56', '2026-04-16 18:45:56', FALSE, 9, 5, 4),
  (40, '01ARZ3NDEKTSV4RRFFQ69G5F17', 'be2c20d8-c5cb-44cf-a833-0ff016d7e5c8', 3, 1, '2026-04-16 17:19:56', '2026-04-16 17:42:56', TRUE, 30, 23, 0),
  (41, '01ARZ3NDEKTSV4RRFFQ69G5F18', '4cb64a41-7219-4b11-8dd4-bb8567e460ad', 3, 2, '2026-04-15 13:39:56', '2026-04-15 13:50:56', TRUE, 33, 25, 6),
  (42, '01ARZ3NDEKTSV4RRFFQ69G5F19', 'a468a837-62fc-44ff-b8c6-d77dc2594337', 3, 4, '2026-04-14 15:25:56', '2026-04-14 16:08:56', TRUE, 34, 20, 2),
  (43, '01ARZ3NDEKTSV4RRFFQ69G5F1A', 'ecd4d10e-bffc-411b-871f-7d16823ec1ab', 3, 2, '2026-04-13 21:30:56', '2026-04-13 21:43:56', TRUE, 27, 14, 5),
  (44, '01ARZ3NDEKTSV4RRFFQ69G5F1B', '4999b3d8-0675-45ac-887e-1b4997f77d9d', 3, 6, '2026-04-14 06:59:56', '2026-04-14 07:12:56', TRUE, 12, 6, 0),
  (45, '01ARZ3NDEKTSV4RRFFQ69G5F1C', 'ab721685-d636-4ffd-ba1b-689abfc7d000', 3, 5, '2026-04-13 12:26:56', '2026-04-13 12:48:56', TRUE, 16, 11, 0),
  (46, '01ARZ3NDEKTSV4RRFFQ69G5F1D', '8c0c083f-7a4b-4f1f-9f1b-8d8a9ba8a5e1', 3, 1, '2026-04-12 17:25:56', '2026-04-12 17:32:56', FALSE, 35, 27, 6),
  (47, '01ARZ3NDEKTSV4RRFFQ69G5F1E', '9f1ecbbb-be37-4dcd-9c5a-a16e0e96c035', 3, 5, '2026-04-12 01:54:56', '2026-04-12 02:07:56', TRUE, 18, 12, 1),
  (48, '01ARZ3NDEKTSV4RRFFQ69G5F1F', '19e74d7e-79b4-468c-b639-7e78755f43d4', 3, 6, '2026-04-12 13:19:56', '2026-04-12 14:02:56', FALSE, 34, 25, 2),
  (49, '01ARZ3NDEKTSV4RRFFQ69G5F1G', '0105a5d1-c3b4-44d9-aad4-504f560a6dca', 3, 3, '2026-04-11 08:17:56', '2026-04-11 08:50:56', FALSE, 24, 14, 4),
  (50, '01ARZ3NDEKTSV4RRFFQ69G5F1H', 'd868bd2a-b78f-4084-b4e8-53f2bfe71da2', 3, 6, '2026-04-10 08:01:56', '2026-04-10 08:26:56', TRUE, 9, 5, 0),
  (51, '01ARZ3NDEKTSV4RRFFQ69G5F1J', '45dbd37e-3a8c-4cd7-9445-8a18cfd8cd41', 4, 5, '2026-05-09 15:42:56', '2026-05-09 16:21:56', FALSE, 31, 11, 4),
  (52, '01ARZ3NDEKTSV4RRFFQ69G5F1K', 'eedcd669-23e2-4af8-9f86-001da6ee879a', 4, 7, '2026-05-09 01:01:56', '2026-05-09 01:12:56', FALSE, 11, 5, 7),
  (53, '01ARZ3NDEKTSV4RRFFQ69G5F1M', '842bce1c-c382-44e0-b066-8e470735a6d6', 4, 2, '2026-05-08 11:49:56', '2026-05-08 12:02:56', FALSE, 7, 4, 5),
  (54, '01ARZ3NDEKTSV4RRFFQ69G5F1N', 'a0ebdce9-d3c5-4269-905d-09ef87426569', 4, 1, '2026-05-08 17:20:56', '2026-05-08 17:50:56', TRUE, 24, 14, 2),
  (55, '01ARZ3NDEKTSV4RRFFQ69G5F1P', '65bfd6ff-705a-4a06-bc21-0c56ecd9c6ca', 4, 7, '2026-05-07 00:45:56', '2026-05-07 00:52:56', TRUE, 20, 9, 6),
  (56, '01ARZ3NDEKTSV4RRFFQ69G5F1Q', 'e821f7ca-7641-4148-8295-07e16abd7892', 4, 2, '2026-05-06 14:10:56', '2026-05-06 14:18:56', FALSE, 19, 7, 7),
  (57, '01ARZ3NDEKTSV4RRFFQ69G5F1R', 'db5811d9-6cc3-4571-8e7f-2f2ac6b72142', 4, 5, '2026-05-06 07:13:56', '2026-05-06 07:46:56', FALSE, 22, 12, 9),
  (58, '01ARZ3NDEKTSV4RRFFQ69G5F1S', '24bad81e-88a0-489b-b6f5-9999307d609e', 4, 7, '2026-05-05 03:53:56', '2026-05-05 04:09:56', FALSE, 20, 10, 3),
  (59, '01ARZ3NDEKTSV4RRFFQ69G5F1T', '7860d15a-56f4-44ca-91f0-537567c2fc0a', 4, 3, '2026-05-04 12:32:56', '2026-05-04 12:54:56', FALSE, 12, 5, 6),
  (60, '01ARZ3NDEKTSV4RRFFQ69G5F1V', '5fe35a19-a9c8-4ca3-a56d-c90aed7bcc18', 4, 2, '2026-05-04 10:24:56', '2026-05-04 10:38:56', FALSE, 12, 5, 5),
  (61, '01ARZ3NDEKTSV4RRFFQ69G5F1W', '03ae922e-9062-419a-b076-82549504ad30', 4, 1, '2026-05-02 03:52:56', '2026-05-02 04:29:56', FALSE, 18, 8, 5),
  (62, '01ARZ3NDEKTSV4RRFFQ69G5F1X', 'ef4a6379-c29f-4f1f-87c5-3613759f4548', 4, 4, '2026-05-01 20:59:56', '2026-05-01 21:34:56', TRUE, 33, 21, 0),
  (63, '01ARZ3NDEKTSV4RRFFQ69G5F1Y', '126aa6c6-44d6-4dba-9414-4322de308a0d', 4, 6, '2026-04-30 03:30:56', '2026-04-30 04:14:56', FALSE, 12, 5, 3),
  (64, '01ARZ3NDEKTSV4RRFFQ69G5F1Z', '559b2006-51ba-403b-83ab-44b1c0709422', 4, 5, '2026-04-30 08:35:56', '2026-04-30 08:37:56', TRUE, 9, 5, 5),
  (65, '01ARZ3NDEKTSV4RRFFQ69G5F20', 'da3e2a61-8c0d-4710-8d5f-26804bacb619', 4, 4, '2026-04-29 02:45:56', '2026-04-29 03:06:56', FALSE, 18, 7, 4),
  (66, '01ARZ3NDEKTSV4RRFFQ69G5F21', 'ac13e068-1574-413e-b195-f45742214cfa', 4, 7, '2026-04-29 14:52:56', '2026-04-29 15:19:56', TRUE, 17, 7, 5),
  (67, '01ARZ3NDEKTSV4RRFFQ69G5F22', 'ed576dde-0269-4c49-808e-4d5a39c4054e', 4, 1, '2026-04-29 12:28:56', '2026-04-29 13:10:56', TRUE, 22, 8, 6),
  (68, '01ARZ3NDEKTSV4RRFFQ69G5F23', '1e7f0690-a609-4321-abe6-7a6453647fb9', 4, 2, '2026-04-26 20:42:56', '2026-04-26 21:12:56', TRUE, 26, 10, 4),
  (69, '01ARZ3NDEKTSV4RRFFQ69G5F24', 'be8f3f1c-e565-4ca2-b56a-0d536c44578d', 4, 1, '2026-04-25 10:11:56', '2026-04-25 10:49:56', TRUE, 27, 10, 7),
  (70, '01ARZ3NDEKTSV4RRFFQ69G5F25', '8357aee4-631f-4066-b3ce-7ff16e7cbad5', 4, 6, '2026-04-25 20:22:56', '2026-04-25 20:35:56', TRUE, 35, 16, 0),
  (71, '01ARZ3NDEKTSV4RRFFQ69G5F26', 'ed13d0f2-8c6d-43c0-8f98-7ac982148b07', 4, 5, '2026-04-23 01:06:56', '2026-04-23 01:09:56', TRUE, 32, 18, 6),
  (72, '01ARZ3NDEKTSV4RRFFQ69G5F27', 'e3c048b8-2c6a-48db-8912-b3844a58f7bf', 4, 1, '2026-04-23 09:47:56', '2026-04-23 10:14:56', TRUE, 21, 11, 1),
  (73, '01ARZ3NDEKTSV4RRFFQ69G5F28', '615416a2-189f-4073-b0e4-00b3e4042c75', 4, 5, '2026-04-22 17:54:56', '2026-04-22 18:36:56', FALSE, 31, 15, 5),
  (74, '01ARZ3NDEKTSV4RRFFQ69G5F29', '2300fde6-2c02-4fd5-98b3-9f7d5ecacb98', 4, 7, '2026-04-22 12:42:56', '2026-04-22 13:20:56', FALSE, 20, 10, 6),
  (75, '01ARZ3NDEKTSV4RRFFQ69G5F2A', 'b13d50a6-84b2-416c-9b3b-2db046b50845', 4, 4, '2026-04-21 13:28:56', '2026-04-21 13:50:56', FALSE, 19, 8, 2),
  (76, '01ARZ3NDEKTSV4RRFFQ69G5F2B', 'dfd4e1a5-24ca-48d9-9f7a-5a24048f21f7', 4, 5, '2026-04-19 15:50:56', '2026-04-19 16:35:56', TRUE, 15, 6, 5),
  (77, '01ARZ3NDEKTSV4RRFFQ69G5F2C', 'dd6d551c-5fc6-45e2-a56e-82cec17f38b8', 4, 2, '2026-04-19 12:46:56', '2026-04-19 13:31:56', TRUE, 7, 3, 2),
  (78, '01ARZ3NDEKTSV4RRFFQ69G5F2D', 'c2f1dbaa-bfb3-42b4-a4a2-7db499481fb0', 4, 2, '2026-04-19 05:40:56', '2026-04-19 06:06:56', FALSE, 5, 1, 6),
  (79, '01ARZ3NDEKTSV4RRFFQ69G5F2E', 'c9966261-c0bf-4700-9060-4e64992a68c8', 4, 3, '2026-04-18 11:39:56', '2026-04-18 12:24:56', FALSE, 21, 9, 7),
  (80, '01ARZ3NDEKTSV4RRFFQ69G5F2F', 'e35a4b10-0992-4587-8317-5da679d68bdf', 4, 1, '2026-04-18 07:04:56', '2026-04-18 07:39:56', TRUE, 28, 11, 4),
  (81, '01ARZ3NDEKTSV4RRFFQ69G5F2G', '96058c56-8926-40da-97ee-b1b9fa2ba5b5', 4, 3, '2026-04-16 23:10:56', '2026-04-16 23:17:56', TRUE, 13, 7, 7),
  (82, '01ARZ3NDEKTSV4RRFFQ69G5F2H', 'a179817c-13ee-4abb-846f-b0af0cd2668a', 4, 2, '2026-04-17 15:03:56', '2026-04-17 15:21:56', TRUE, 5, 2, 4),
  (83, '01ARZ3NDEKTSV4RRFFQ69G5F2J', '372f0b51-0593-452f-96f3-7947ad244d0c', 4, 1, '2026-04-17 20:18:56', '2026-04-17 20:55:56', FALSE, 9, 5, 5),
  (84, '01ARZ3NDEKTSV4RRFFQ69G5F2K', '7055da37-e173-49ca-b94f-434283c4abb6', 4, 4, '2026-04-16 05:53:56', '2026-04-16 06:01:56', TRUE, 13, 8, 4),
  (85, '01ARZ3NDEKTSV4RRFFQ69G5F2M', '1ae43f4c-29f8-4d3c-b62f-f90b5a192aae', 4, 1, '2026-04-15 08:50:56', '2026-04-15 09:06:56', TRUE, 6, 2, 6),
  (86, '01ARZ3NDEKTSV4RRFFQ69G5F2N', '178ec28d-460c-46b0-a622-08f14fabd51e', 4, 7, '2026-04-15 17:46:56', '2026-04-15 18:20:56', FALSE, 30, 16, 0),
  (87, '01ARZ3NDEKTSV4RRFFQ69G5F2P', 'd1633f5a-abd2-4feb-b7d9-df8834ba6c08', 4, 1, '2026-04-14 07:02:56', '2026-04-14 07:16:56', FALSE, 14, 7, 9),
  (88, '01ARZ3NDEKTSV4RRFFQ69G5F2Q', '3084db0f-0c05-43ca-bab9-e3d52dbc0ac6', 4, 1, '2026-04-13 12:39:56', '2026-04-13 13:06:56', TRUE, 22, 8, 6),
  (89, '01ARZ3NDEKTSV4RRFFQ69G5F2R', '4fa899ae-c967-4751-a267-60c94655e953', 4, 1, '2026-04-13 06:37:56', '2026-04-13 07:21:56', FALSE, 12, 5, 4),
  (90, '01ARZ3NDEKTSV4RRFFQ69G5F2S', 'fe724694-276a-4cc2-875e-19c9e0381e0f', 4, 4, '2026-04-12 12:31:56', '2026-04-12 12:39:56', FALSE, 30, 19, 8),
  (91, '01ARZ3NDEKTSV4RRFFQ69G5F2T', 'c2c0f2c6-4da3-4e82-aee9-a1c9cee06c43', 4, 7, '2026-04-11 01:12:56', '2026-04-11 01:49:56', FALSE, 7, 2, 5),
  (92, '01ARZ3NDEKTSV4RRFFQ69G5F2V', 'c9b19bb6-4676-4a87-9bfe-b3d378b51b99', 4, 3, '2026-04-11 11:53:56', '2026-04-11 12:26:56', TRUE, 27, 13, 2),
  (93, '01ARZ3NDEKTSV4RRFFQ69G5F2W', '147b2760-d746-4fbf-8562-53c7a6cc1717', 4, 6, '2026-04-10 07:16:56', '2026-04-10 07:53:56', FALSE, 28, 13, 4),
  (94, '01ARZ3NDEKTSV4RRFFQ69G5F2X', '0477a373-e104-4888-aec1-fe128019ae4c', 4, 7, '2026-04-10 03:20:56', '2026-04-10 03:32:56', FALSE, 13, 7, 5);

SELECT setval('student_sessions_id_seq', 94, true);

-- Constraints for table `ai_interactions`
ALTER TABLE ai_interactions
  ADD CONSTRAINT ai_interactions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT ai_interactions_exercise_id_fkey FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE SET NULL ON UPDATE CASCADE;

-- --------------------------------------------------------

--
-- Additional indexes for foreign keys
--

CREATE INDEX exercises_user_id_idx ON exercises (user_id);
CREATE INDEX exercises_lesson_id_idx ON exercises (lesson_id);
CREATE INDEX lessons_parent_id_idx ON lessons (parent_id);
CREATE INDEX users_rank_id_idx ON users (rank_id);
CREATE INDEX ai_config_user_id_idx ON ai_config (last_updated_by);

-- Trigger function for automatic last_edit management
CREATE OR REPLACE FUNCTION update_last_edit() RETURNS TRIGGER LANGUAGE plpgsql AS 'BEGIN NEW.last_edit = clock_timestamp(); RETURN NEW; END';

CREATE OR REPLACE TRIGGER user_ranks_set_last_edit
    BEFORE UPDATE ON user_ranks
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER users_set_last_edit
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER lessons_set_last_edit
    BEFORE UPDATE ON lessons
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER exercises_set_last_edit
    BEFORE UPDATE ON exercises
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER comments_set_last_edit
    BEFORE UPDATE ON comments
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER comment_flags_set_last_edit
    BEFORE UPDATE ON comment_flags
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_groups_set_last_edit
    BEFORE UPDATE ON user_groups
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER user_groups_meta_set_last_edit
    BEFORE UPDATE ON user_groups_meta
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER student_sessions_set_last_edit
    BEFORE UPDATE ON student_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER ai_interactions_set_last_edit
    BEFORE UPDATE ON ai_interactions
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();

CREATE OR REPLACE TRIGGER ai_config_set_last_edit
    BEFORE UPDATE ON ai_config
    FOR EACH ROW
    EXECUTE FUNCTION update_last_edit();
