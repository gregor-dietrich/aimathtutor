# TODO - Detailed Implementation Plans

## 0. General

### Implementation Priority

1. **Graspable Math Action Validation (isValidAction)** (Task 7)
   Requires math parsing/normalization or CAS integration, careful testing and rollout.

2. **Multiple Problems Per Exercise** (Task 2)
   Involves DB changes, session tracking, and sequential UI logic.

3. **AdminConfigView: Runtime AI Provider/Model/Settings Management** (Task 5)
   *Requires dynamic config management, secure runtime updates, and advanced UI/UX for admin settings.

4. **Gamification** (Task 6)
   Backend entities, rules, and careful UI/UX and privacy considerations.

5. **Miscellaneous Fixes** (Task 4)

### Testing Checklist (for each feature)

- [ ] Unit tests for service methods
- [ ] Integration tests for DB operations
- [ ] Manual UI testing in both views
- [ ] Edge cases (empty data, invalid input, etc.)
- [ ] Permission/security checks
- [ ] Performance with large datasets (admin views)

---

## 2. Multiple Problems Per Exercise with Sequential Unlocking

**Goal:** Allow exercises to have multiple problems (like hints), unlock "Next Problem" button when current is complete.

**Implementation Plan:**

### 2.1 Backend Changes

1. **ExerciseEntity/ExerciseViewDto** - Modify fields:
   - `graspableInitialExpression` → Keep as is (semicolon-separated: "2x+5=15;3x-7=20;x^2=9")
   - `graspableTargetExpression` → Add this field (semicolon-separated: "x=5;x=9;x=3")
   - Parse expressions by splitting on `;` or `|`

2. **GraspableMathService** - Add session tracking:
   - `StudentSessionEntity.currentProblemIndex` (new field, default 0)
   - Track which problem in the sequence student is working on
   - Method: `int getCurrentProblemIndex(String sessionId)`
   - Method: `void advanceToNextProblem(String sessionId)`

3. **Database Changes:**
   - Add `current_problem_index INT DEFAULT 0` to `student_sessions` table
   - Add `graspable_target_expression VARCHAR(1000)` to `exercises` table
   - Add them to the existing init script - do NOT create separate scripts

### 2.2 Frontend Changes

1. **ExerciseWorkspaceView** - Add UI components:
   - Field: `int currentProblemIndex = 0`
   - Field: `String[] problems` (parsed from `exercise.graspableInitialExpression`)
   - Field: `String[] targetExpressions` (parsed from `exercise.graspableTargetExpression`)
   - Button: `nextProblemButton` (initially disabled)

2. **Problem Navigation:**
   - Load problem at index `currentProblemIndex` initially
   - When `checkCompletion()` returns true:
     - Enable `nextProblemButton` if `currentProblemIndex < problems.length - 1`
     - Disable canvas interactions until next problem loaded
   - On "Next Problem" click:
     - Increment `currentProblemIndex`
     - Call `graspableMathService.advanceToNextProblem(sessionId)`
     - Load next problem expression
     - Disable button again, re-enable canvas
   - Display progress: "Problem 2 of 3" in hints section

3. **Completion State:**
   - When last problem is completed, show final success message
   - Mark entire session as complete in database
   - Show "Back to Exercises" or "Review Session" options

4. **Admin/Teacher View**
   - Exercise creation form: Add help text explaining semicolon-separated format
   - Example: "2x+5=15;3x-7=20" → Two problems in sequence

---

## 4. Miscellaneous Fixes

### 4.1 Admin Dashboard Enhancement

The admin dashboard could use some further enhancement, such as diagrams.

### 4.2 Keyboard accessibility

Clickable spans are used extensively across views, especially admin views, however tehy lack keyboard accessibility. Users navigating with keyboards cannot trigger the click event. Consider using a Button or Anchor component with appropriate ARIA attributes, or add keyboard event listeners (Enter/Space) to the Span.

### 4.3 Pagination

Add server-side pagination for admin views to handle large datasets gracefully (Vaadin data provider + backend query offsets).

### 4.4 Unit Test Coverage

Unit Test Coverage should be reviewed and probably improved across multiple packages.

### 4.5 Security Considerations

### 4.5.1 ULIDs

Use ULIDs for IDs rather than auto-incrementing integers.

### 4.5.2 Rate Limits

Implement rate limits. These should be configurable with generous but sensible defaults.

### 4.6 Database Tweaks

Review init.sql as well as all queries in the project for optimization potential. Notable examples would be `filterByDateRange` methods.

### 4.7 Refactoring: Database Access

Refactor services and entities so that services no longer include database queries, maintaining separation of concerns between layers.

---

## 5. AdminConfigView: Runtime AI Provider/Model/Settings Management

**Goal:** Create a new `AdminConfigView` (route: `admin/config`) allowing users with admin privileges to manage AI tutor configuration at runtime. This replaces static `application.properties` changes for AI settings, enabling dynamic provider switching and parameter tuning without redeployment.

**Context:**

- Current AI configuration is entirely static via `@ConfigProperty` injections in `AiTutorService`, `GeminiAiService`, `OpenAiService`, and `OllamaService`.
- Quarkus `@ConfigProperty` does NOT support runtime updates by default — values are injected once at startup.
- Runtime configuration requires either:
  1. A custom configuration entity stored in the database (preferred for persistence and multi-instance deployments).
  2. Use of `ConfigProvider` API with a custom `ConfigSource` (more complex, less user-friendly for persistence).
- We strongly favor the custom configuration entity stored in the database due to lower complexity and better handling for persistence.
- Current admin views: `AdminDashboardView`, `AdminUsersView`, `AdminExercisesView`, etc. — no config view exists yet.
- **No backward compatibility:** All `@ConfigProperty` fields and hardcoded constants will be removed. Configuration must be managed via database only.

**Implementation Plan:**

### 5.1 Database Schema Changes

1. **New Entity: `AiConfigEntity`**
   - Fields:
     - `id` (Long, primary key)
     - `configKey` (String, unique index) — e.g., "ai.tutor.provider", "gemini.model", "ai.prompt.math.tutoring.prefix"
     - `configValue` (String or TEXT for longer prompts) — e.g., "gemini", "gemini-2.5-flash-lite", or full prompt text
     - `configType` (String) — data type hint: "STRING", "INTEGER", "DOUBLE", "BOOLEAN", "TEXT"
     - `category` (String) — grouping: "GENERAL", "GEMINI", "OPENAI", "OLLAMA", "PROMPTS"
     - `description` (String, nullable) — human-readable help text
     - `lastUpdatedAt` (LocalDateTime)
     - `lastUpdatedBy` (Long, FK to `users.id`)
   - Add table `ai_config` to existing `init.sql` (do NOT create separate migration scripts per project style).
   - Seed with current defaults from `application.properties` AND hardcoded prompts from `AiTutorService` so existing deployments have baseline config.
   - **Configuration Keys to Add (in database):**
     - `ai.tutor.enabled` — e.g., "true"
     - `ai.tutor.provider` — e.g., "gemini", "openai", "ollama", "mock"
     - `gemini.model` — e.g., "gemini-2.5-flash-lite"
     - `gemini.api.base-url` — e.g., "<https://generativelanguage.googleapis.com>"
     - `gemini.temperature` — e.g., "0.7"
     - `gemini.max-tokens` — e.g., "1000"
     - `openai.model` — e.g., "gpt-4o-mini"
     - `openai.organization-id` — e.g., "" (optional)
     - `openai.api.base-url` — e.g., "<https://api.openai.com/v1>"
     - `openai.temperature` — e.g., "0.7"
     - `openai.max-tokens` — e.g., "1000"
     - `ollama.api.url` — e.g., "<http://localhost:11434>"
     - `ollama.model` — e.g., "llama3.1:8b"
     - `ollama.temperature` — e.g., "0.7"
     - `ollama.max-tokens` — e.g., "1000"
     - `ollama.timeout-seconds` — e.g., "30"
     - `ai.prompt.question.answering.prefix` — currently `questionAnsweringPromptPrefix` in `AiTutorService`
     - `ai.prompt.question.answering.postfix` — currently `questionAnsweringPromptPostfix` in `AiTutorService`
     - `ai.prompt.math.tutoring.prefix` — currently `mathTutoringPromptPrefix` in `AiTutorService`
     - `ai.prompt.math.tutoring.postfix` — currently `mathTutoringPromptPostfix` in `AiTutorService`
   - **API Keys remain as `@ConfigProperty` via environment variables:**
     - `gemini.api.key` — injected via `@ConfigProperty`, sourced from `GEMINI_API_KEY` environment variable
     - `openai.api.key` — injected via `@ConfigProperty`, sourced from `OPENAI_API_KEY` environment variable
     - No changes to existing API key handling

2. **Repository: `AiConfigRepository`**
   - Extend `PanacheRepositoryBase<AiConfigEntity, Long>`
   - Methods:
     - `Optional<AiConfigEntity> findByConfigKey(String key)`
     - `List<AiConfigEntity> findByCategory(String category)`
     - `void upsert(String key, String value, String type, String category, Long userId)` — insert or update

### 5.2 Backend Service Layer

1. **New Service: `AiConfigService` (@ApplicationScoped)**
   - Injections:
     - `AiConfigRepository`
     - `AuthService` (for permission checks)
   - Methods:
     - `String getConfigValue(String key, String defaultValue)` — fetches from DB, falls back to default
     - `Integer getConfigValueAsInt(String key, Integer defaultValue)`
     - `Double getConfigValueAsDouble(String key, Double defaultValue)`
     - `Boolean getConfigValueAsBoolean(String key, Boolean defaultValue)`
     - `void updateConfig(String key, String value, Long userId)` (@Transactional) — validates, updates DB, invalidates cache
     - `Map<String, String> getAllConfigsByCategory(String category)` — returns key-value map for UI population
     - `void validateAndSave(Map<String, String> configUpdates, Long userId)` — bulk validation and save
   - **Caching:** Use `@CacheResult` on read methods with invalidation on updates to avoid DB hits per AI request.
   - **Validation:** Check format (e.g., URL format for API endpoints, numeric ranges for temperature/max-tokens).
   - **Note:** The `defaultValue` parameters in getter methods are used ONLY during initial seeding or if admin explicitly triggers a "reset to defaults" action. All normal operations must read from database.

2. **Refactor AI Services to Use `AiConfigService`**
   - `AiTutorService`:
     - Inject `AiConfigService`
     - **Remove ALL `@ConfigProperty` fields** (`aiEnabled`, `aiProvider`)
     - **Remove ALL hardcoded prompt constants** (`questionAnsweringPromptPrefix`, `questionAnsweringPromptPostfix`, `mathTutoringPromptPrefix`, `mathTutoringPromptPostfix`)
     - Replace with dynamic loading from `AiConfigService`:
       - Check enabled status: `aiConfigService.getConfigValueAsBoolean("ai.tutor.enabled", true)`
       - Get provider: `aiConfigService.getConfigValue("ai.tutor.provider", "mock")`
       - In `buildQuestionAnsweringPrompt()`: fetch prefix/postfix via `aiConfigService.getConfigValue("ai.prompt.question.answering.prefix", "")` and `aiConfigService.getConfigValue("ai.prompt.question.answering.postfix", "")`
       - In `buildMathTutoringPrompt()`: fetch prefix/postfix via `aiConfigService.getConfigValue("ai.prompt.math.tutoring.prefix", "")` and `aiConfigService.getConfigValue("ai.prompt.math.tutoring.postfix", "")`
       - If any config value is missing/empty, throw `IllegalStateException` with clear error message directing admin to configure via UI
   - `GeminiAiService`, `OpenAiService`, `OllamaService`:
     - Inject `AiConfigService`
     - **Keep existing `@ConfigProperty` for API keys** — no changes to current API key injection
     - Replace other config fields with `AiConfigService` calls (e.g., `model = aiConfigService.getConfigValue("gemini.model", "")`, `temperature = aiConfigService.getConfigValueAsDouble("gemini.temperature", 0.7)`)
     - Throw `IllegalStateException` if required config (model, URL, etc.) is missing
     - Fetch config values per-request or on-demand rather than caching in instance fields to ensure runtime updates take effect immediately.

3. **DTOs:**
   - `AiConfigDto`: `{ configKey, configValue, configType, category, description, lastUpdatedAt, lastUpdatedBy }`
   - `AiConfigUpdateDto`: `{ configKey, configValue }` (for batch updates from UI)

### 5.3 Frontend Changes

1. **New View: `AdminConfigView`**
   - Route: `@Route(value = "admin/config", layout = AdminMainLayout.class)`
   - Page Title: `@PageTitle("AI Configuration - AI Math Tutor")`
   - Inject: `AiConfigService`, `AuthService`
   - Layout: Tabbed interface or accordion for categories (General, Gemini, OpenAI, Ollama).

2. **UI Components (General Settings Tab):**
   - **AI Tutor Enabled:** Checkbox bound to `ai.tutor.enabled`
   - **AI Provider:** ComboBox with options: `mock`, `gemini`, `openai`, `ollama`
     - On selection change, update visible provider-specific tabs/sections

3. **UI Components (Provider-Specific Tabs):**
   - **Gemini Tab:**
     - API Key: PasswordField (masked, placeholder: "API key is managed via environment variable `GEMINI_API_KEY`", disabled/read-only)
     - Model: TextField (default: `gemini-2.5-flash-lite`, editable)
     - API Base URL: TextField (default: `https://generativelanguage.googleapis.com`, editable)
     - Temperature: NumberField (0.0-2.0, step 0.1, default 0.7, editable)
     - Max Tokens: IntegerField (1-4096, default 1000, editable)
     - Help text: Link to <https://aistudio.google.com/app/apikey>
   - **OpenAI Tab:**
     - API Key: PasswordField (masked, placeholder: "API key is managed via environment variable `OPENAI_API_KEY`", disabled/read-only)
     - Organization ID: TextField (optional, placeholder: "Enter org ID if applicable", editable)
     - Model: TextField (default: `gpt-4o-mini`, editable)
     - API Base URL: TextField (default: `https://api.openai.com/v1`, editable)
     - Temperature: NumberField (0.0-2.0, step 0.1, default 0.7, editable)
     - Max Tokens: IntegerField (1-4096, default 1000, editable)
     - Help text: Link to <https://platform.openai.com/api-keys>
   - **Ollama Tab:**
     - API URL: TextField (default: `http://localhost:11434`, editable)
     - Model: TextField (default: `llama3.1:8b`, editable)
     - Temperature: NumberField (0.0-2.0, step 0.1, default 0.7, editable)
     - Max Tokens: IntegerField (1-4096, default 1000, editable)
     - Timeout (seconds): IntegerField (1-300, default 30, editable)
     - Help text: Link to <https://ollama.com/download> with note: "Ollama does not require an API key"
   - **Prompts Tab:**
     - **Question Answering Prompt Prefix:** TextArea (multi-line, expandable, default: current `questionAnsweringPromptPrefix` value)
     - **Question Answering Prompt Postfix:** TextArea (multi-line, expandable, default: current `questionAnsweringPromptPostfix` value)
     - **Math Tutoring Prompt Prefix:** TextArea (multi-line, expandable, default: current `mathTutoringPromptPrefix` value)
     - **Math Tutoring Prompt Postfix:** TextArea (multi-line, expandable, default: current `mathTutoringPromptPostfix` value)
     - Help text: Explain that these prompts are sent to the AI provider and control the AI's behavior/tone. Include character count indicators for each field.
     - **Preview Button:** Shows a dialog with example prompt construction using current values (helps admins understand how prompts are assembled).

4. **UI Behavior:**
   - Load current config from `AiConfigService.getAllConfigsByCategory()` on view init.
   - Disable provider-specific tabs if provider not selected (grayed out or hidden).
   - Show warning icon/tooltip next to API key fields if value is empty or placeholder (`"your-api-key-here"`).
   - **Save Button:** Validates all fields, calls `AiConfigService.validateAndSave()`, shows success notification or error details.
   - **Reset to Defaults Button:** Restores `application.properties` defaults (optional feature).
   - **Test Connection Button (per provider):** Sends a test prompt to verify API key/URL/model work (calls respective AI service with a dummy prompt, shows result in dialog).

5. **Animations & Polish:**
   - Smooth expand/collapse for provider-specific sections (Vaadin `Details` component or custom CSS transitions).
   - Field validation with immediate feedback (e.g., red border + tooltip for invalid URL format).
   - Loading indicators during save/test operations.

### 5.4 Security & Validation

1. **Permissions:**
   - Only users with `rankId = 1` (Admin) can access `/admin/config` route.
   - Enforce via `@BeforeEnterObserver` in view: check `authService.getCurrentUser().rankId == 1`.
   - Add server-side validation in `AiConfigService.updateConfig()` to double-check admin rank before persisting.

2. **Input Validation:**
   - URLs: Valid HTTP/HTTPS format, reachable (optional DNS check).
   - Temperature: 0.0 ≤ value ≤ 2.0
   - Max Tokens: 1 ≤ value ≤ 8192 (adjust per provider limits)
   - Timeout: 1 ≤ value ≤ 300 seconds
   - Model names: Non-empty strings, no special characters except hyphen/underscore.
   - **Prompts:** Non-empty strings, reasonable length limits (e.g., 10-5000 characters per prompt field to prevent abuse/performance issues).

3. **Sensitive Data Handling:**
   - API keys are managed via `@ConfigProperty` reading from environment variables — no database storage.
   - Do NOT log API keys in service methods (mask in logs).

4. **Runtime Effect:**
   - Config changes take effect **immediately** for new AI interactions (no restart required).
   - Existing in-flight requests use old config (acceptable).
   - Cache invalidation ensures fresh config reads within seconds (tune cache TTL if needed).

### 5.5 Testing

1. **Unit Tests:**
   - `AiConfigServiceTest`: Test CRUD operations, validation logic, fallback to defaults, type conversions.
   - Mock `AiConfigRepository` to verify upsert, findByKey, and caching behavior.
   - Test prompt loading and fallback to hardcoded defaults when DB entries missing.

2. **Integration Tests:**
   - `AiConfigServiceIT`: Test with real H2/PostgreSQL DB, verify transactions, concurrent updates, cache invalidation.
   - Test that `AiTutorService` picks up config changes without restart (mock DB config change, call `analyzeMathAction()`, verify correct provider used).
   - Test that prompt changes take effect immediately in `buildQuestionAnsweringPrompt()` and `buildMathTutoringPrompt()` methods.

3. **UI Tests:**
   - Manual: Navigate to `/admin/config`, change provider, verify provider-specific fields show/hide correctly.
   - Manual: Edit prompts in Prompts tab, save, trigger AI interaction, verify new prompts are used.
   - Manual: Use "Preview" button to verify prompt assembly logic with current configuration.
   - Manual: Save config, restart app (optional), verify settings persist and AI interactions use new config.
   - Manual: Test "Test Connection" for each provider (requires valid API keys or mock responses).

4. **Edge Cases:**
   - Missing DB entries: Application should fail fast with clear error messages directing admin to configure settings via UI.
   - Invalid config values (should reject with clear error messages).
   - Concurrent admin updates (last-write-wins, consider optimistic locking if needed).
   - Very long prompts (test performance impact, enforce reasonable limits).

### 5.6 Migration & Rollout

1. **Deployment Steps:**
   - Add `ai_config` table to `init.sql` with seed data matching current `application.properties` AND hardcoded prompt values from `AiTutorService`.
   - Remove all `@ConfigProperty` fields from `AiTutorService`, `GeminiAiService`, `OpenAiService`, and `OllamaService` EXCEPT API key fields.
   - Remove hardcoded prompt constants from `AiTutorService`.
   - All configuration except API keys must be loaded from database via `AiConfigService` — no fallbacks.
   - Deploy updated services and `AdminConfigView` together (atomic deployment).

2. **Phased Rollout:**
   - Phase 1: Implement `AiConfigEntity`, `AiConfigService`, and DB-backed config loading. Test with direct DB inserts. Include prompt configuration keys.
   - Phase 2: Build `AdminConfigView` with basic fields (provider, model, API key). Enable for admins.
   - Phase 3: Add Prompts tab with TextArea fields for all four prompt components (prefix/postfix for question answering and math tutoring).
   - Phase 4: Add advanced features (test connection, preview prompt assembly, reset to defaults, audit log of config changes).

### 5.7 Future Enhancements (Out of Scope for Initial Implementation)

- **Audit Log:** Track all config changes in separate `ai_config_audit` table (who, when, old/new values).
- **Multi-Tenancy:** Per-group or per-user AI config overrides (requires more complex config resolution logic).
- **API Key Encryption:** Encrypt API keys at rest using Quarkus Vault or similar.
- **Provider Health Monitoring:** Dashboard showing uptime, latency, error rates per provider.
- **Cost Tracking:** Log tokens used per provider, estimate costs, set budget alerts.
- **Prompt Versioning:** Track prompt changes over time, allow rollback to previous versions.
- **A/B Testing:** Support multiple prompt variants, track which performs better based on student outcomes.
- **Prompt Templates:** Library of pre-built prompts for different tutoring styles (Socratic, encouraging, strict, etc.).
- **Prompt Variables:** Support dynamic placeholders in prompts (e.g., `{{student_name}}`, `{{difficulty_level}}`) that get replaced at runtime.

---

## 6. Gamification

**Goal:** Increase student motivation and engagement by adding gamification elements such as achievements/badges, progress levels, experience points (XP), streaks, leaderboards, and rewards tied to problem solving within the Graspable Math workspace and overall course progress.

This feature should be opt-in per user (privacy-friendly), configurable by admins, and designed to be low-friction so it does not interfere with learning objectives.

### 6.1 High-level features

- Achievements/Badges: award for specific milestones (e.g., "First Solution", "10 Problems Solved", "Perfect Session", "Fast Solver", "Hint Avoider").
- Experience points (XP): reward XP for solved problems, streaks, and completing exercises. XP contributes to user Level.
- Levels & Progress Bar: users level up based on XP thresholds; show a progress bar on dashboard and exercise view.
- Daily Streaks: consecutive days with activity—rewards and streak badges.
- Leaderboards: global and class/group leaderboards showing top XP or most problems solved. Respect privacy settings (opt-in/opt-out, show anonymized handles).
- Challenges & Quests: time-limited or teacher-assigned challenges (e.g., "Solve 5 linear equations this week") with rewards.
- Rewards & Unlocks: unlock cosmetic rewards (avatars, themes), extra practice problems, or hints currency that can be spent.
- Notifications & Activity Feed: notify users when they earn badges, level up, or climb the leaderboard.

### 6.2 Backend changes

1. New Entities (Hibernate/Panache style):
   - `BadgeEntity` - id, code, name, description, iconPath, criteriaJson, createdAt
   - `UserBadgeEntity` - id, userId, badgeId, awardedAt, source (auto/manual)
   - `UserXpEntity` - id, userId, totalXp, level, nextLevelXp, lastUpdated
   - `UserStreakEntity` - id, userId, currentStreakDays, lastActiveDate
   - `ChallengeEntity` - id, title, description, startDate, endDate, rewardXp, rewardBadgeId, createdBy
   - `UserChallengeEntity` - id, userId, challengeId, progressJson, completedAt
   - `LeaderboardSnapshotEntity` (optional) - snapshotDate, rankingJson (for caching)

2. Service classes:
   - `GamificationService` (@ApplicationScoped)
     - awardBadge(userId, badgeCode, source)
     - addXp(userId, amount, reason)
     - incrementStreak(userId, date)
     - getUserBadges(userId)
     - getUserXpAndLevel(userId)
     - getLeaderboards(scope, groupId, limit)
     - evaluateAndAwardOnProblemSolved(sessionId, eventDto) — called from GraspableMathService or AITutorService when problems are solved
   - `ChallengeService` - create/manage challenges, track user progress

3. DTOs
   - `BadgeDto`, `UserBadgeDto`, `UserXpDto`, `ChallengeDto`, `LeaderboardDto`

4. DB migrations / schema updates
   - Add tables for each new entity. As per project style, add fields to existing init scripts (do NOT add separate scripts).
   - Add indexes on `userId` and `badgeCode` where helpful.

5. Integration points
   - Call `GamificationService.evaluateAndAwardOnProblemSolved(...)` from `GraspableMathService` whenever a problem is marked complete.
   - Call `addXp(...)` when user actions qualify (fast solve bonus, no-hint bonus, perfect session).
   - Update `StudentSessionEntity` to optionally record `xpEarned` for the session and `badgesAwardedJson` (or rely on `UserBadgeEntity`).

### 6.3 Frontend changes (Vaadin views)

1. New Views/Components
   - `GamificationPanel` component: compact widget to show current level, XP progress bar, recent badges, and quick action to view full gamification profile.
   - `BadgesView` (@Route "badges") - list of all badges with filters (earned/not earned), and badge details.
   - `LeaderboardView` (@Route "leaderboard") - toggle between global, class/group, and friends.
   - `ChallengesView` (@Route "challenges") - list active/past challenges and allow users to join (if allowed).
   - Integrate small toast/notification UI in `ExerciseWorkspaceView` for immediate feedback when a badge is earned or XP awarded.

2. UI behavior
   - Show XP progress bar in the main user dashboard and in `ExerciseWorkspaceView` (top-right corner) so users can see immediate progress.
   - When a badge is earned, show a celebratory modal/toast with badge icon and description; include an unobtrusive "share" option (copy link or classroom share).
   - Leaderboard toggles to respect privacy: anonymize names if user opted out of public rankings.
   - Provide settings in `UserProfileView` for gamification opt-in/out and visibility preferences.

3. Admin Controls
   - Extend `AdminConfigView` (or new `AdminGamificationView`) to manage badges, XP rules, level thresholds, challenge creation, and leaderboard settings.
   - Allow admins/teachers to award badges manually.

### 6.4 XP, Levels, and Rules (example policy)

- Base XP per solved problem: 10 XP
- Bonus: +5 XP for solving without hints
- Speed bonus: up to +10 XP proportional to time under expected time
- Streak bonus: +2 XP per consecutive day active (capped)
- Challenge completion: rewardXp per challenge config
- Level thresholds: exponential or pre-configured table (e.g., Level 1: 0 XP, Level 2: 100 XP, Level 3: 300 XP, Level 4: 700 XP)

Keep rules configurable via `AdminGamificationView`.

### 6.5 Privacy & Accessibility

- Gamification must be opt-in for students; default can be enabled but provide a clear toggle in profile.
- Allow students to hide their name from leaderboards (opt-out) and to use an alias.
- Ensure badges and colors are accessible (contrast, screen-reader friendly alt text for icons).

### 6.6 Testing

- Unit tests for `GamificationService` (award logic, XP calculations, level progression).
- Integration tests for DB writes (badge awards, XP updates, streak increments).
- UI tests for badge modal display and leaderboard filtering.
- Load testing/benchmarks for leaderboard queries (cache snapshots if needed).

### 6.7 Metrics & Analytics

- Track gamification engagement metrics: percent of users opting in, average XP earned per session, badge earn rates, churn/retention impact.
- Add events to existing logging/analytics pipeline (e.g., `GAMIFICATION_BADGE_AWARDED`, `GAMIFICATION_XP_ADDED`).

### 6.8 Phased rollout and migration

- Phase 1 (MVP): XP, badges for a small default set (First Solution, 10 Problems, No Hints), user opt-in, basic UI panel, and admin config for enabling/disabling.
- Phase 2: Add leaderboards, challenges, rewards/unlocks, and teacher tools.
- Phase 3: Advanced features like seasonal events, classroom competitions, and integration with external LMS.

### 6.9 Risks and mitigations

- Reward focus over learning: design badges to align tightly with learning goals (e.g., accuracy, explanation, reflection), not just speed.
- Privacy concerns: defaults and opt-outs must be clear and honored.
- Cheating via repeated trivial tasks: weight XP and badges to discourage grinding (e.g., cap repeatable XP per day for the same exercise).

---

Update task statuses in project tracking and prepare follow-up issues for implementation.

## 7. Graspable Math Action Validation (isValidAction)

Goal: Implement server-side validation of student math actions coming from the Graspable Math workspace so that session metrics (correctActions, success rate) reflect true mathematical correctness rather than relying solely on the frontend or marking every action as correct.

Why this is separate: The repository currently sets `event.correct = true` for all math actions in `ExerciseWorkspaceView`. The quick note in `ISSUES.md` explains that adding a robust `isValidAction(before, after)` is non-trivial and was deferred. This TODO captures a concrete plan to implement it as a discrete task.

Priority: Medium — important for analytics accuracy but non-trivial and requires careful selection of a math parsing/normalization approach.

Estimated difficulty: ★★★★☆ (parsing and canonicalizing math expressions reliably is tricky)

Implementation plan:

- Backend changes (core):
   1. Add a new method to `GraspableMathService`:
       - `public Boolean isValidAction(String expressionBefore, String expressionAfter)`
       - Returns `null` if action significance is undetermined, `true` if the transformation is mathematically valid, `false` if invalid.
   2. Implement a normalization/parsing strategy used by both `isValidAction()` and existing `checkCompletion()`:
       - Option A (preferred): integrate a lightweight symbolic math library that can parse and compare expressions (examples: Symja, exp4j with extensions, or a small custom CAS). Evaluate licensing and size impact.
       - Option B: Implement deterministic normalization heuristics (whitespace removal, canonical ordering of commutative terms, simple algebraic normalization like expand/sort/factor for common patterns). This is lower-cost but brittle and should be documented as such.
   3. If using a library, add the dependency to `pom.xml` and write an adapter class (e.g., `MathExpressionComparator`) to centralize parsing/normalization logic.
   4. Update `ExerciseWorkspaceView.onMathAction(...)` to call `event.correct = this.graspableMathService.isValidAction(expressionBefore, expressionAfter);` and handle `null` (unknown) by leaving prior behavior or marking as false depending on a configurable policy.

- Backend changes (data/metrics):
   1. Ensure `StudentSessionEntity` handling in `GraspableMathService.processEvent()` handles `null`/`false` properly (do not increment correctActions for `false` or `null` if policy dictates).
   2. Add config toggles or feature flags (admin-settable) to control strictness: strict (treat unknown as incorrect), lenient (treat unknown as correct), or roll-out mode (log only).

- Tests:
   1. Unit tests for `MathExpressionComparator` / normalization adapter: pairs of expressions that should be equal/unequal (e.g., `2x+3` vs `3+2x`, `x=5` vs `5=x`, `(x+1)(x+2)` vs `x^2+3x+2`, basic fraction reductions, basic simplifications).
   2. Integration tests for `GraspableMathService.isValidAction()` using typical event samples from frontend fixtures.
   3. End-to-end test: simulate `onMathAction()` calls and assert session `correctActions` increments according to expectations.

- Migration/compatibility notes:
   1. If a third-party CAS is added, verify Quarkus runtime compatibility and packaging size. Consider making the dependency optional behind a feature profile.
   2. Document limitations (supported operations, edge cases) in developer docs and in `ISSUES.md` so maintainers and teachers understand where validation may be conservative.

- Rollout suggestion:
   1. Phase 1 (Log-only): Implement `isValidAction()` and log results, but do not change `correctActions` counting. Use logs to tune heuristics/cases.
   2. Phase 2 (Opt-in strictness): Add admin toggle; enable strict mode for a subset of exercises or pilot classrooms.
   3. Phase 3 (Default enforcement): Once stable, make stricter behavior the default.

Owner: Backend team / person familiar with symbolic math libraries

Deliverables:

- `GraspableMathService.isValidAction()` implementation
- `MathExpressionComparator` (or adapter) with unit tests
- Updated `ExerciseWorkspaceView.onMathAction(...)` usage and tests
- Admin toggle or config for validation policy

Timebox suggestion: 3-6 person-days to prototype/evaluate libraries, implement MVP heuristics, and create tests. If a full CAS integration is required, add time for design and vetting (additional 1-2 weeks).
