# TODO - Detailed Implementation Plans

## 0. General

### Implementation Priority

### Suggested Order (Easiest to Hardest)

1. **Comments on Exercises** (Task 4)
   *Moderately Complex*: Backend and frontend work, DB schema verification, moderation, and realtime updates.

2. **Multiple Problems Per Exercise** (Task 2)
   *Moderately Complex*: Involves DB changes, session tracking, and sequential UI logic.

3. **Admin Views for Progress Tracking** (Task 3)
   *Very Complex*: Multiple new views, analytics, charts, security checks, and extensive backend/frontend integration.

4. **AdminConfigView: Runtime AI Provider/Model/Settings Management** (Task 5)
   *Complex*: Requires dynamic config management, secure runtime updates, and advanced UI/UX for admin settings.

5. **Gamification** (Task 6)
   *Moderately Complex*: Backend entities, rules, and careful UI/UX and privacy considerations.

**Difficulty Ratings:**

- Task 2: ★★★☆☆
- Task 3: ★★★★★
- Task 4: ★★★☆☆
- Task 5: ★★★★☆
- Task 6: ★★★☆☆

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

## 3. Admin Views for Progress Tracking

**Goal:** Create admin-only views to monitor student sessions, AI interactions, and overall progress.

**Implementation Plan:**

### 3.1 Backend Changes

1. **New Service:** `AnalyticsService.java` (@ApplicationScoped)
   - `List<StudentSessionViewDto> getAllSessions()`
   - `List<StudentSessionViewDto> getSessionsByUser(Long userId)`
   - `List<StudentSessionViewDto> getSessionsByExercise(Long exerciseId)`
   - `List<AIInteractionViewDto> getAIInteractionsBySession(String sessionId)`
   - `StudentProgressSummaryDto getUserProgressSummary(Long userId)`
   - `Map<String, Integer> getProblemCategoryStats()` (how many problems solved per category)

2. **New DTOs:**
   - `StudentSessionViewDto` (expand existing with user/exercise names)
   - `AIInteractionViewDto` (event type, feedback given, timestamp)
   - `StudentProgressSummaryDto`:
     - `Long userId`, `String username`
     - `int totalSessions`, `int completedSessions`
     - `int totalProblems`, `int completedProblems`
     - `int hintsUsed`, `int averageActionsPerProblem`
     - `LocalDateTime lastActivity`

3. **Entity Enhancement:**
   - Ensure `AIInteractionEntity` has all needed fields:
     - `sessionId`, `eventType`, `feedbackMessage`, `timestamp`

### 3.2 Frontend Changes

1. **New View:** `AdminDashboardView.java` (@Route "admin/dashboard")
   - Check user rank permissions (`rank.adminView == true`)
   - Display overview cards:
     - Total sessions today/week/month
     - Active students
     - Most attempted exercises
   - Charts/graphs (use Vaadin Charts if available, or simple tables)

2. **New View:** `StudentSessionsView.java` (@Route "admin/sessions")
   - Grid displaying all sessions with filters:
     - Columns: Student, Exercise, Start Time, Duration, Completed, Hints Used, Actions
     - Filter by: Student (dropdown), Exercise (dropdown), Date range, Completion status
   - Click row → Navigate to detailed session view

3. **New View:** `SessionDetailView.java` (@Route "admin/session/:sessionId")
   - Display complete session timeline:
     - Each action taken (expression before/after)
     - AI feedback given for each action
     - Hints revealed
     - Time spent on each step
   - Reconstruct the student's problem-solving path
   - Show final outcome (completed/abandoned)

4. **New View:** `StudentProgressView.java` (@Route "admin/progress")
   - Grid of all students with summary statistics
   - Columns: Username, Sessions, Completed, Success Rate, Last Activity
   - Click row → Detailed student profile with:
     - Session history
     - Strengths/weaknesses analysis (based on problem categories)
     - Time trends (improving/struggling)

5. **Navigation:**
   - Add "Admin" tab to MainLayout navigation bar (visible only if `rank.adminView == true`)
   - Submenu: Dashboard, Sessions, Student Progress

### 3.3 Security

- Add checks in `beforeEnter()` for all admin views:

  ```java
  if (!authService.hasAdminView()) {
      event.rerouteTo(HomeView.class);
      NotificationUtil.showError("Access denied");
  }
  ```

---

## 4. Comments on Exercises (student comments & discussion)

**Goal:** Let students comment on exercises and lessons, support threaded discussions, moderation and admin management. The repository already contains some comment-related entities and admin views; this task documents what is still missing and provides a concrete implementation checklist to fully support comments across backend, frontend, database, admin moderation, and tests.

### Current state (as reported)

- Entities and admin views for comments already exist in the codebase (verify by searching for `CommentEntity`, `CommentService`, `CommentView` in the project).
- What appears to be missing or incomplete: DTOs for view models, a well-defined `CommentService` contract (create/edit/delete/list/moderate), Vaadin components on the exercise view to display and post comments, DB init schema additions (if entities exist but not in init scripts), realtime updates (server push) for new comments, and comprehensive tests.

### Key requirements (high level)

- Students can create comments on exercises and lessons.
- Comments should support replies (threading) and simple formatting (plain text, optional markdown-lite).
- Users can edit/delete their own comments within a short window; teachers/admins can edit/delete/moderate all comments.
- Admins can view, filter, hide/unhide, and permanently delete comments via existing admin views.
- Comments must be associated with exerciseId, lessonId (optional), sessionId (optional), userId, timestamp, and moderation status.
- Real-time UI updates (Vaadin server push or polling) so students see new comments without full page refresh.
- Audit/logging of moderation actions and comment deletes.
- Rate-limiting/anti-spam and optional flagging flow for user-reported comments.

### Missing pieces (detailed analysis)

1. Comment DTOs and view models
   - `CommentDto` (for create/update requests) with fields: `exerciseId`, `lessonId` (optional), `parentCommentId` (nullable), `text`.
   - `CommentViewDto` (for rendering) with fields: `id`, `exerciseId`, `lessonId`, `parentId`, `authorId`, `authorName`, `text`, `createdAt`, `editedAt`, `status` (VISIBLE/HIDDEN/DELETED), `sessionId`, `flagsCount`.

2. CommentService contract and implementation
   - Methods to implement (ApplicationScoped service):
     - `CommentViewDto createComment(CommentDto dto, Long authorId)`
     - `CommentViewDto editComment(Long commentId, CommentDto dto, Long editorId)`
     - `void deleteComment(Long commentId, Long requesterId, boolean softDelete)` (soft delete preserves audit trail)
     - `List<CommentViewDto> listCommentsByExercise(Long exerciseId, int page, int size, Optional<Long> parentId)`
     - `List<CommentViewDto> listCommentsBySession(String sessionId)`
     - `void moderateComment(Long commentId, ModerationAction action, Long moderatorId, String reason)`
     - `void flagComment(Long commentId, Long userId, String reason)`
   - Security checks: only author or admin can edit/delete (respect soft-delete window), moderators/admins can override.

3. Database / schema
   - If `CommentEntity` exists, ensure the init SQL includes the table definition (columns and constraints). If not present, add to the existing init script (do NOT create a separate migration script per project guidelines). Required columns:
     - `id` (PK), `exercise_id` (FK nullable), `lesson_id` (FK nullable), `session_id` (varchar nullable), `parent_comment_id` (FK nullable), `author_id` (FK), `text` (text), `created_at` (timestamp), `edited_at` (timestamp nullable), `status` (varchar, default 'VISIBLE'), `flags_count` (int default 0), `deleted_by` (nullable), `deleted_at` (nullable)
   - Indexes: `exercise_id`, `lesson_id`, `parent_comment_id`, `author_id` for efficient queries.

4. Vaadin UI integration
   - Exercise view (`ExerciseWorkspaceView` / `GraspableMathView`):
     - Add a `CommentsPanel` component (threaded list + new comment form) placed beside or below the Graspable canvas.
     - The component should request initial comments via a server call (injected `CommentService`) and subscribe to server push for new comment events.
     - Comment composer: text area, submit button, optional reply-to handling, client-side validation, character limit.
     - For authors: show edit/delete controls; for moderators: show moderate/hide/delete controls.
     - Pagination or lazy-loading of long threads.

5. Real-time updates and notifications
   - Implement Vaadin push (if enabled) or fallback to short polling (e.g., fetch new comments every X seconds).
   - Optionally fire `UI.access()` updates when `CommentService.createComment` is called from server side or when a WebSocket broadcast announces a new comment.
   - Publish comment events for users in the same exercise/session using an application-scoped broadcaster (e.g., CDI Event or small message hub).

6. Admin moderation & audit
   - Ensure admin views include filters for comments by exercise, author, status, flagged count, and date range.
   - Add audit logging for moderation actions (who hid/deleted/edited, timestamp, reason). Link to `AIInteractionEntity` or a dedicated `CommentModerationEntity` if audit trail is required.

7. Spam, rate limiting, and abuse mitigation
   - Implement basic rate limiting per user (e.g., 1 comment per 5s, 200 comments/day) in `CommentService`.
   - Add flagging workflow: when flagged by N users, auto-hide or queue for review.
   - Optionally integrate a third-party moderation API (AI-based profanity/spam check) at create time.

8. Tests
   - Unit tests for `CommentService` (create/edit/delete/list/moderation, permission checks, rate limits).
   - Integration tests that exercise DB writes and queries (session/comment linking, parent/child thread behavior).
   - UI tests for `CommentsPanel` (posting, replying, edit/delete visibility based on permissions).

9. Documentation & admin help
   - Update `README.md` and `docs/Quickstart.md` (if necessary) with short note about comments (privacy, moderation rules).
   - Add an entry to developer docs describing the comment schema and how to wire the `CommentsPanel` into exercise views.

### Implementation checklist (step-by-step)

1. Audit repository for existing comment artifacts
   - Search for `CommentEntity`, `CommentService`, `CommentController`, `CommentView` and note which parts are implemented. Record filenames.

2. Add/verify DTOs
   - Create `CommentDto.java` and `CommentViewDto.java` in `src/main/java/de/vptr/aimathtutor/dto/` if missing.

3. Implement/complete `CommentService` (@ApplicationScoped)
   - Implement methods listed above with permission checks, rate limiting, and flagging support.

4. Ensure DB schema/init scripts include comment table
   - Add required columns and indexes into the existing SQL init script used by the project.

5. Vaadin `CommentsPanel` component
   - Build a reusable component that can be embedded into `ExerciseWorkspaceView` and other places.
   - Support pagination, replying, edit/delete controls, and moderation UI for admins.

6. Real-time notifications
   - Add a simple in-app broadcaster (CDI Event or small hub) to notify sessions of new comments; implement Vaadin push handler or polling fallback.

7. Admin views & moderation
   - Hook into existing admin views: add comment filters, list, and moderation actions.

8. Tests
   - Add unit and integration tests. Run `mvn test` and fix issues.

9. Docs & README
   - Add short usage and privacy note.

### Edge cases & additional notes

- Consider whether comments should be versioned or soft-deleted (recommended to soft-delete and keep audit).
- Consider attachments/images later — start with text-only.
- Consider rate-limits, abuse reporting, and export of comment data for teachers.
- Performance: paginate and index heavily; for very large classes consider caching/recent-only view.

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
