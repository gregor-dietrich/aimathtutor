# TODO - Detailed Implementation Plans

## 0. General

### Implementation Priority

### Suggested Order (Easiest to Hardest)

1. **Multiple Problems Per Exercise** (Task 2)
   *Moderately Complex*: Involves DB changes, session tracking, and sequential UI logic.

2. **AdminConfigView: Runtime AI Provider/Model/Settings Management** (Task 5)
   *Complex*: Requires dynamic config management, secure runtime updates, and advanced UI/UX for admin settings.

3. **Gamification** (Task 6)
   *Complex*: Backend entities, rules, and careful UI/UX and privacy considerations.

**Difficulty Ratings:**

- Task 2: ★★★☆☆
- Task 5: ★★★★☆
- Task 6: ★★★★☆

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

## 5. AdminConfigView: Runtime AI Provider/Model/Settings Management

**Goal:** Transform the admin home view into `AdminConfigView`, allowing users with admin privileges to change application-wide AI settings at runtime.

**Implementation Plan:**

### 5.1 Backend Changes

1. **Config Properties:**
   - Rename `ai.tutor.provider` → `ai.tutor.default.provider`
   - Rename `gemini.model` → `gemini.default.model`
   - Rename `openai.model` → `openai.default.model`
   - Rename `ollama.model` → `ollama.default.model`
   - Add unified properties for `ai.tutor.max-tokens` and `ai.tutor.temperature` (not per-provider)
   - Add `openai.organization-id`, `ollama.timeout-seconds`, and provider-specific API URLs

2. **Service:**
   - Add service for updating config properties at runtime (with validation and security checks)

### 5.2 Frontend Changes

1. **AdminConfigView:**
   - Replace admin home view with a config panel for AI settings
   - Dropdowns for AI provider and model (model dropdown updates when provider changes)
   - Disable Gemini/OpenAI if API key is unset ("your-api-key-here"), disable Ollama if URL is unset ("your-ollama-api-url-here")
   - Inputs for max-tokens and temperature (always visible, affect selected provider)
   - Inputs for OpenAI organization ID and Ollama timeout (hidden unless respective provider selected, ideally with smooth animation)
   - Input for API URL (affects only selected provider)

2. **UI/UX:**
   - Show/hide provider-specific fields with subtle animation
   - Ensure only one set of model/temperature/max-tokens fields, always reflecting selected provider

### 5.3 Security & Validation

1. Only users with admin privileges can access and change settings
2. Validate all inputs before saving
3. Changes should take effect immediately for new AI interactions

### 5.4 Testing

- Unit tests for config update service
- Integration tests for runtime config changes
- Manual UI testing for all provider/model combinations and field visibility

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
