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
(3, '01ARZ3NDEKTSV4RRFFQ69G5FG2', 'gemini.model', 'gemma-3-27b-it', 'STRING', 'GEMINI', 'Gemini model name', false, 1),
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
