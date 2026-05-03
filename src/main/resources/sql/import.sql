-- PostgreSQL initialization script

-- --------------------------------------------------------

--
-- Inserts for table `user_ranks`
--

INSERT INTO user_ranks (id, version, name, admin_view, exercise_add, exercise_delete, exercise_edit, lesson_add, lesson_delete, lesson_edit, comment_add, comment_delete, comment_edit, user_add, user_delete, user_edit, user_group_add, user_group_delete, user_group_edit, user_rank_add, user_rank_delete, user_rank_edit) VALUES
(1, 0, 'Admin', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),
(2, 0, 'Teacher', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
(3, 0, 'Student', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE);

-- Set sequence to 3 so next value is 4
SELECT setval('user_ranks_id_seq', 3, true);

-- --------------------------------------------------------

--
-- Inserts for table `users`
--

INSERT INTO users (id, version, username, password, rank_id, activated, banned) VALUES
(1, 0, 'admin', '$2b$12$w8ewBFQZep0CEQDxh4p.9Oaor5uen7aCzkKdSQfKbrk4o9RPFk./O', 1, TRUE, FALSE),
(2, 0, 'teacher', '$2b$12$E68NPoLky4lHFGj0OxdYje.QAZkBeUBwhpBukumxE48Gbp1rMmxoK', 2, TRUE, FALSE),
(3, 0, 'student1', '$2b$12$QwEJzk2meYHQHbVAFgMOVeEO0PH5pcf98J/waFLClvhiM7kglefZK', 3, TRUE, FALSE),
(4, 0, 'student2', '$2b$12$Xu5uhklcRKHKY0DNrEuWBOP/9aJX.gZdJaJ3KR2.YonJgogb3zTlu', 3, TRUE, FALSE);

-- Set sequence to 4 so next value is 5
SELECT setval('users_id_seq', 4, true);

-- --------------------------------------------------------

-- Inserts for table `lessons`

INSERT INTO lessons (id, version, name, parent_id) VALUES
(1, 0, 'Algebra', NULL),
(2, 0, 'Linear Equations', 1),
(3, 0, 'Quadratic Equations', 1),
(4, 0, 'Polynomials', 1);

-- Set sequence to 4 so next value is 5
SELECT setval('lessons_id_seq', 4, true);

-- --------------------------------------------------------

-- Seed exercises for lessons (graspable-enabled where appropriate)

INSERT INTO exercises (id, version, title, content, user_id, lesson_id, published, commentable, graspable_enabled, graspable_initial_expression, graspable_target_expression, graspable_difficulty, graspable_hints)
VALUES
  (1, 0, 'Solve for x: simple linear', 'Solve the equation for x: 2x + 3 = 11', 2, 2, TRUE, TRUE, TRUE, '2*x + 3 = 11', 'x = 4', 'BEGINNER', '["Isolate the term with x","Subtract 3 from both sides","Divide both sides by 2"]'),
  (2, 0, 'Two-step linear equation', 'Solve: 3(x - 2) = 9', 2, 2, TRUE, TRUE, TRUE, '3*(x - 2) = 9', 'x = 5', 'BEGINNER', '["Divide both sides by 3","Then add 2 to both sides"]'),
  (3, 0, 'Linear equation with fractions', 'Solve: (1/2)x + 1 = 4', 2, 2, TRUE, TRUE, TRUE, '(1/2)*x + 1 = 4', 'x = 6', 'INTERMEDIATE', '["Eliminate fractions by multiplying both sides","Isolate x"]'),
  (4, 0, 'Expand and simplify', 'Expand and simplify the expression (x + 2)(x - 3).', 2, 4, TRUE, TRUE, TRUE, '(x + 2)*(x - 3)', 'x^2 - x - 6', 'INTERMEDIATE', '["Use distributive property","Combine like terms"]'),
  (5, 0, 'Solve quadratic by factoring', 'Solve for x by factoring: x^2 - 5x + 6 = 0', 2, 3, TRUE, TRUE, TRUE, 'x^2 - 5*x + 6 = 0', 'x = 2 or x = 3', 'INTERMEDIATE', '["Find two numbers that multiply to 6 and add to -5","Set each factor to zero"]'),
  (6, 0, 'Complete the square', 'Solve by completing the square: x^2 + 6x + 5 = 0', 2, 3, TRUE, TRUE, TRUE, 'x^2 + 6*x + 5 = 0', 'x = -1 or x = -5', 'ADVANCED', '["Move constant to the right","Add (b/2)^2 to both sides","Take square root of both sides"]'),
  (7, 0, 'Quadratic formula', 'Use the quadratic formula to solve: 2x^2 - 4x - 6 = 0', 2, 3, TRUE, TRUE, TRUE, '2*x^2 - 4*x - 6 = 0', 'x = 2 or x = -1.5', 'ADVANCED', '["Identify a, b, c","Apply the quadratic formula","Simplify the results"]');

INSERT INTO exercises (id, version, title, content, user_id, lesson_id, published, commentable, graspable_enabled)
VALUES
  (8, 0, 'Standalone Exercise', 'This exercise is not in any category and does not have Graspable Math enabled. Just for testing.', 2, NULL, TRUE, TRUE, FALSE);

-- Set sequence to 8 so next value is 9
SELECT setval('exercises_id_seq', 8, true);

-- --------------------------------------------------------

--
-- Inserts for table `user_groups`
--

INSERT INTO user_groups (id, version, name) VALUES
(1, 0, 'Teacher'),
(2, 0, 'Class 8A'),
(3, 0, 'Class 8B'),
(4, 0, 'Class 9A'),
(5, 0, 'Class 9B');

-- Set sequence to 5 so next value is 6
SELECT setval('user_groups_id_seq', 5, true);

-- --------------------------------------------------------

--
-- Inserts for table `user_groups_meta`
--

INSERT INTO user_groups_meta (id, user_id, group_id) VALUES
(1, 2, 1),
(2, 3, 4),
(3, 4, 4);

-- Set sequence to 3 so next value is 4
SELECT setval('user_groups_meta_id_seq', 3, true);

-- --------------------------------------------------------

-- Seed AI configuration with defaults from application.properties and hardcoded prompts
INSERT INTO ai_config (version, config_key, config_value, config_type, category, description, is_optional, last_updated_by) VALUES
-- General settings
(0, 'ai.tutor.enabled', 'true', 'BOOLEAN', 'GENERAL', 'Enable or disable AI tutor functionality', false, 1),
(0, 'ai.tutor.provider', 'mock', 'STRING', 'GENERAL', 'AI provider to use: mock, gemini, openai, or ollama', false, 1),

-- Gemini settings
(0, 'gemini.model', 'gemma-3-27b-it', 'STRING', 'GEMINI', 'Gemini model name', false, 1),
(0, 'gemini.api.base-url', 'https://generativelanguage.googleapis.com', 'STRING', 'GEMINI', 'Gemini API base URL', false, 1),
(0, 'gemini.temperature', '0.7', 'DOUBLE', 'GEMINI', 'Gemini temperature setting (0.0-2.0)', false, 1),
(0, 'gemini.max-tokens', '2000', 'INTEGER', 'GEMINI', 'Gemini maximum tokens for responses', false, 1),

-- OpenAI settings
(0, 'openai.model', 'gpt-5-nano', 'STRING', 'OPENAI', 'OpenAI model name', false, 1),
(0, 'openai.organization-id', '', 'STRING', 'OPENAI', 'OpenAI organization ID (optional)', true, 1),
(0, 'openai.api.base-url', 'https://api.openai.com/v1', 'STRING', 'OPENAI', 'OpenAI API base URL', false, 1),
(0, 'openai.temperature', '0.7', 'DOUBLE', 'OPENAI', 'OpenAI temperature setting (0.0-2.0)', false, 1),
(0, 'openai.max-tokens', '2000', 'INTEGER', 'OPENAI', 'OpenAI maximum tokens for responses', false, 1),

-- Ollama settings
(0, 'ollama.api.url', 'http://ollama:11434', 'STRING', 'OLLAMA', 'Ollama API URL', false, 1),
(0, 'ollama.model', 'llama3.2:3b', 'STRING', 'OLLAMA', 'Ollama model name', false, 1),
(0, 'ollama.temperature', '0.7', 'DOUBLE', 'OLLAMA', 'Ollama temperature setting (0.0-2.0)', false, 1),
(0, 'ollama.max-tokens', '2000', 'INTEGER', 'OLLAMA', 'Ollama maximum tokens for responses', false, 1),
(0, 'ollama.timeout-seconds', '30', 'INTEGER', 'OLLAMA', 'Ollama API timeout in seconds', false, 1),

-- Prompt settings
(0, 'ai.prompt.question.answering.prefix', 'You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question.', 'TEXT', 'PROMPTS', 'Prefix prompt for question answering', false, 1),
(0, 'ai.prompt.question.answering.postfix', 'Provide a helpful, encouraging answer that:
- Guides the student''s thinking without solving it for them
- Is concise (2-3 sentences max)
- Relates to their current problem if possible
- Uses clear, simple language
- Encourages them to try the next step

Your answer:', 'TEXT', 'PROMPTS', 'Postfix prompt for question answering', false, 1),
(0, 'ai.prompt.math.tutoring.prefix', 'You are an encouraging but concise AI math tutor helping a student learn algebra. Analyze the student''s action and provide brief, helpful feedback.', 'TEXT', 'PROMPTS', 'Prefix prompt for math tutoring', false, 1),
(0, 'ai.prompt.math.tutoring.postfix', 'Provide feedback in the following JSON format:
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
