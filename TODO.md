# TODO - Detailed Implementation Plans

## 0. General

### Implementation Priority

1. **Graspable Math Action Validation (isValidAction)** (Task 7)
   *Complex*: Requires math parsing/normalization or CAS integration, careful testing and rollout.

2. **Multiple Problems Per Exercise** (Task 2)
   *Moderately Complex*: Involves DB changes, session tracking, and sequential UI logic.

3. **AdminConfigView: Runtime AI Provider/Model/Settings Management** (Task 5)
   *Complex*: Requires dynamic config management, secure runtime updates, and advanced UI/UX for admin settings.

4. **Gamification** (Task 6)
   *Very Complex*: Backend entities, rules, and careful UI/UX and privacy considerations.

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

### 4.1 Grid Improvements

### 4.1.1 Grid Row Sorting

Current behavior: Grid rows in admin views are sorted properly on inital load, but editing items places them at the very bottom upon saving.

Expected behavior: Grid rows in admin views should always use `ORDER BY 'id' DESC`.

### 4.1.2 Count Updates

In various admin views, computed columns, such as users in a group for user groups, or child lessons for lessons, are not updated automatically after actions in the admin view actually change the correct value, e.g. adding a new child lesson to a parent lesson does not update the count until refreshing the page or clicking the fresh button. The issue is likely the same one across admin views that have such columns.

### 4.2 Admin Dashboard Enhancement

The admin dashboard could use some further enhancement, such as diagrams.

### 4.3 Keyboard accessibility

Clickable spans are used extensively across views, especially admin views, however tehy lack keyboard accessibility. Users navigating with keyboards cannot trigger the click event. Consider using a Button or Anchor component with appropriate ARIA attributes, or add keyboard event listeners (Enter/Space) to the Span.

### 4.4 Pagination

Add server-side pagination for admin views to handle large datasets gracefully (Vaadin data provider + backend query offsets).

### 4.5 Unit Test Coverage

Unit Test Coverage should be reviewed and probably improved across multiple packages.

### 4.6 Security Considerations

### 4.6.1 ULIDs

Use ULIDs for IDs rather than auto-incrementing integers.

### 4.6.2 Rate Limits

Implement rate limits. These should be configurable with generous but sensible defaults.

### 4.7 Database Tweaks

Review init.sql as well as all queries in the project for optimization potential. Notable examples would be `filterByDateRange` methods.

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
