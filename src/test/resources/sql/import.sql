-- PostgreSQL initialization script

-- --------------------------------------------------------

--
-- Inserts for table `user_ranks`
--

INSERT INTO user_ranks (id, name, admin_view, exercise_add, exercise_delete, exercise_edit, lesson_add, lesson_delete, lesson_edit, comment_add, comment_delete, comment_edit, user_add, user_delete, user_edit, user_group_add, user_group_delete, user_group_edit, user_rank_add, user_rank_delete, user_rank_edit) VALUES
(1, 'Admin', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE),
(2, 'Teacher', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE),
(3, 'Student', FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE);

-- Set sequence to 3 so next value is 4
SELECT setval('user_ranks_id_seq', 3, true);

-- --------------------------------------------------------

--
-- Inserts for table `users`
--

INSERT INTO users (id, username, password, salt, rank_id, activated) VALUES
(1, 'admin', '3HWqMv8tiSEbBcsUfxqBx7kY4vw+cSvG7OQXp9uzM0w=', '0l/SGC6gqKwYWjw7sm2IrwzIcAjq/QkO9xXcG/LC56c=', 1, TRUE),
(2, 'teacher', 'gqjX9Myv2T0+cSsc7Mk5uP00vWN74acNaV8aVJvvK8Q=', 'Oz3c7v4qJJqqbPHlTzAhilp4O7o+DdW4iBYQMJRABQo=', 2, TRUE),
(3, 'student1', 't/NeeExH/6i3y2DBq77LXyOkGvnk6TCaE1p/lLObE98=', 'tpINgKObPWkbOrylflSrEECZi5ZHvhv2Wjkzlr9HW3E=', 3, TRUE),
(4, 'student2', '0hCDh1yJvbG4VDOqtZWF3qgL3YPUYneknACoEQ6G8Kc=', '4G1YeLz6tsTH98j9zOoEcxvSK0uZnM51uLhF6O6H7pM=', 3, TRUE);

-- Set sequence to 4 so next value is 5
SELECT setval('users_id_seq', 4, true);

-- --------------------------------------------------------

-- Inserts for table `lessons`

INSERT INTO lessons (id, name, parent_id) VALUES
(1, 'Algebra', NULL),
(2, 'Linear Equations', 1),
(3, 'Quadratic Equations', 1),
(4, 'Polynomials', 1);

-- Set sequence to 4 so next value is 5
SELECT setval('lessons_id_seq', 4, true);

-- --------------------------------------------------------

-- Seed exercises for lessons (graspable-enabled where appropriate)

INSERT INTO exercises (id, title, content, user_id, lesson_id, published, commentable, graspable_enabled, graspable_initial_expression, graspable_target_expression, graspable_difficulty, graspable_hints)
VALUES
  (1, 'Solve for x: simple linear', 'Solve the equation for x: 2x + 3 = 11', 2, 2, TRUE, TRUE, TRUE, '2*x + 3 = 11', 'x = 4', 'beginner', '["Isolate the term with x","Subtract 3 from both sides","Divide both sides by 2"]'),
  (2, 'Two-step linear equation', 'Solve: 3(x - 2) = 9', 2, 2, TRUE, TRUE, TRUE, '3*(x - 2) = 9', 'x = 5', 'beginner', '["Divide both sides by 3","Then add 2 to both sides"]'),
  (3, 'Linear equation with fractions', 'Solve: (1/2)x + 1 = 4', 2, 2, TRUE, TRUE, TRUE, '(1/2)*x + 1 = 4', 'x = 6', 'intermediate', '["Eliminate fractions by multiplying both sides","Isolate x"]'),
  (4, 'Expand and simplify', 'Expand and simplify the expression (x + 2)(x - 3).', 2, 4, TRUE, TRUE, TRUE, '(x + 2)*(x - 3)', 'x^2 - x - 6', 'intermediate', '["Use distributive property","Combine like terms"]'),
  (5, 'Solve quadratic by factoring', 'Solve for x by factoring: x^2 - 5x + 6 = 0', 2, 3, TRUE, TRUE, TRUE, 'x^2 - 5*x + 6 = 0', 'x = 2 or x = 3', 'intermediate', '["Find two numbers that multiply to 6 and add to -5","Set each factor to zero"]'),
  (6, 'Complete the square', 'Solve by completing the square: x^2 + 6x + 5 = 0', 2, 3, TRUE, TRUE, TRUE, 'x^2 + 6*x + 5 = 0', 'x = -1 or x = -5', 'advanced', '["Move constant to the right","Add (b/2)^2 to both sides","Take square root of both sides"]'),
  (7, 'Quadratic formula', 'Use the quadratic formula to solve: 2x^2 - 4x - 6 = 0', 2, 3, TRUE, TRUE, TRUE, '2*x^2 - 4*x - 6 = 0', 'x = 2 or x = -1.5', 'advanced', '["Identify a, b, c","Apply the quadratic formula","Simplify the results"]');

INSERT INTO exercises (id, title, content, user_id, lesson_id, published, commentable, graspable_enabled)
VALUES
  (8, 'Standalone Exercise', 'This exercise is not in any category and does not have Graspable Math enabled. Just for testing.', 2, NULL, TRUE, TRUE, FALSE);

-- Set sequence to 8 so next value is 9
SELECT setval('exercises_id_seq', 8, true);

-- --------------------------------------------------------

--
-- Inserts for table `user_groups`
--

INSERT INTO user_groups (id, name) VALUES
(1, 'Teacher'),
(2, 'Class 8A'),
(3, 'Class 8B'),
(4, 'Class 9A'),
(5, 'Class 9B');

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
INSERT INTO ai_config (config_key, config_value, config_type, category, description, is_optional, last_updated_by) VALUES
-- General settings
('ai.tutor.enabled', 'true', 'BOOLEAN', 'GENERAL', 'Enable or disable AI tutor functionality', false, 1),
('ai.tutor.provider', 'mock', 'STRING', 'GENERAL', 'AI provider to use: mock, gemini, openai, or ollama', false, 1),

-- Gemini settings
('gemini.model', 'gemini-2.5-flash-lite', 'STRING', 'GEMINI', 'Gemini model name', false, 1),
('gemini.api.base-url', 'https://generativelanguage.googleapis.com', 'STRING', 'GEMINI', 'Gemini API base URL', false, 1),
('gemini.temperature', '0.7', 'DOUBLE', 'GEMINI', 'Gemini temperature setting (0.0-2.0)', false, 1),
('gemini.max-tokens', '1000', 'INTEGER', 'GEMINI', 'Gemini maximum tokens for responses', false, 1),

-- OpenAI settings
('openai.model', 'gpt-4o-mini', 'STRING', 'OPENAI', 'OpenAI model name', false, 1),
('openai.organization-id', '', 'STRING', 'OPENAI', 'OpenAI organization ID (optional)', true, 1),
('openai.api.base-url', 'https://api.openai.com/v1', 'STRING', 'OPENAI', 'OpenAI API base URL', false, 1),
('openai.temperature', '0.7', 'DOUBLE', 'OPENAI', 'OpenAI temperature setting (0.0-2.0)', false, 1),
('openai.max-tokens', '1000', 'INTEGER', 'OPENAI', 'OpenAI maximum tokens for responses', false, 1),

-- Ollama settings
('ollama.api.url', 'http://ollama:11434', 'STRING', 'OLLAMA', 'Ollama API URL', false, 1),
('ollama.model', 'llama3.2:3b', 'STRING', 'OLLAMA', 'Ollama model name', false, 1),
('ollama.temperature', '0.7', 'DOUBLE', 'OLLAMA', 'Ollama temperature setting (0.0-2.0)', false, 1),
('ollama.max-tokens', '1000', 'INTEGER', 'OLLAMA', 'Ollama maximum tokens for responses', false, 1),
('ollama.timeout-seconds', '30', 'INTEGER', 'OLLAMA', 'Ollama API timeout in seconds', false, 1),

-- Prompt settings
('ai.prompt.question.answering.prefix', 'You are a helpful AI math tutor. A student is working on an algebra problem and has asked you a question.', 'TEXT', 'PROMPTS', 'Prefix prompt for question answering', false, 1),
('ai.prompt.question.answering.postfix', 'Provide a helpful, encouraging answer that:
- Guides the student''s thinking without solving it for them
- Is concise (2-3 sentences max)
- Relates to their current problem if possible
- Uses clear, simple language
- Encourages them to try the next step

Your answer:', 'TEXT', 'PROMPTS', 'Postfix prompt for question answering', false, 1),
('ai.prompt.math.tutoring.prefix', 'You are an encouraging but concise AI math tutor helping a student learn algebra. Analyze the student''s action and provide brief, helpful feedback.', 'TEXT', 'PROMPTS', 'Prefix prompt for math tutoring', false, 1),
('ai.prompt.math.tutoring.postfix', 'Provide feedback in the following JSON format:
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
