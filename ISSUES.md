# TODO - Detailed Implementation Plans

## 0. General

### Testing Checklist (for each feature)

- [ ] Unit tests for service methods
- [ ] Integration tests for DB operations
- [ ] Manual UI testing in both views
- [ ] Edge cases (empty data, invalid input, etc.)
- [ ] Permission/security checks
- [ ] Performance with large datasets (admin views)

---

## 1. Gamification

**Goal:** Increase student motivation and engagement by adding gamification elements such as achievements/badges, progress levels, experience points (XP), streaks, leaderboards, and rewards tied to problem solving within the Graspable Math workspace and overall course progress.

This feature should be opt-in per user (privacy-friendly), configurable by admins, and designed to be low-friction so it does not interfere with learning objectives.

### 1.1 High-level features

- Achievements/Badges: award for specific milestones (e.g., "First Solution", "10 Problems Solved", "Perfect Session", "Fast Solver", "Hint Avoider").
- Experience points (XP): reward XP for solved problems, streaks, and completing exercises. XP contributes to user Level.
- Levels & Progress Bar: users level up based on XP thresholds; show a progress bar on dashboard and exercise view.
- Daily Streaks: consecutive days with activity—rewards and streak badges.
- Leaderboards: global and class/group leaderboards showing top XP or most problems solved. Respect privacy settings (opt-in/opt-out, show anonymized handles).
- Challenges & Quests: time-limited or teacher-assigned challenges (e.g., "Solve 5 linear equations this week") with rewards.
- Rewards & Unlocks: unlock cosmetic rewards (avatars, themes), extra practice problems, or hints currency that can be spent.
- Notifications & Activity Feed: notify users when they earn badges, level up, or climb the leaderboard.

### 1.2 Backend changes

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

### 1.3 Frontend changes (Vaadin views)

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

### 1.4 XP, Levels, and Rules (example policy)

- Base XP per solved problem: 10 XP
- Bonus: +5 XP for solving without hints
- Speed bonus: up to +10 XP proportional to time under expected time
- Streak bonus: +2 XP per consecutive day active (capped)
- Challenge completion: rewardXp per challenge config
- Level thresholds: exponential or pre-configured table (e.g., Level 1: 0 XP, Level 2: 100 XP, Level 3: 300 XP, Level 4: 700 XP)

Keep rules configurable via `AdminGamificationView`.

### 1.5 Privacy & Accessibility

- Gamification must be opt-in for students; default can be enabled but provide a clear toggle in profile.
- Allow students to hide their name from leaderboards (opt-out) and to use an alias.
- Ensure badges and colors are accessible (contrast, screen-reader friendly alt text for icons).
- **Data deletion and retention policies:**
  - **Right to Deletion:** When a student opts out of gamification or their account is deleted, all personally identifiable gamification data must be removed or anonymized. Specifically:
    - `UserBadgeEntity`, `UserXpEntity`, `UserStreakEntity`, `UserChallengeEntity` — implement cascade delete or soft-delete/anonymization (replace user reference with a synthetic anonymized ID) so that aggregate statistics remain valid while individual identity is removed.
  - **Retention periods:**
    - Leaderboard snapshots: retain current + 12 months, then archive or fully anonymize (strip names/aliases, keep only rank and score distributions).
    - Challenge participation history: retain for the lifetime of the challenge + 6 months, then anonymize or purge.
    - Badge-award audit trails: retain for 24 months for abuse investigation, then purge.
  - **Implementation note:** Add soft-deletion/anonymization support in the relevant entities and services. Provide admin tools or API endpoints to process deletion requests and to export/delete user data on demand.

### 1.6 Testing

- Unit tests for `GamificationService` (award logic, XP calculations, level progression).
- Integration tests for DB writes (badge awards, XP updates, streak increments).
- UI tests for badge modal display and leaderboard filtering.
- Load testing/benchmarks for leaderboard queries (cache snapshots if needed).

### 1.7 Metrics & Analytics

- Track gamification engagement metrics: percent of users opting in, average XP earned per session, badge earn rates, churn/retention impact.
- Add events to existing logging/analytics pipeline (e.g., `GAMIFICATION_BADGE_AWARDED`, `GAMIFICATION_XP_ADDED`).

### 1.8 Phased rollout and migration

- Phase 1 (MVP): XP, badges for a small default set (First Solution, 10 Problems, No Hints), user opt-in, basic UI panel, and admin config for enabling/disabling.
- Phase 2: Add leaderboards, challenges, rewards/unlocks, and teacher tools.
- Phase 3: Advanced features like seasonal events, classroom competitions, and integration with external LMS.

### 1.9 Risks and mitigations

- Reward focus over learning: design badges to align tightly with learning goals (e.g., accuracy, explanation, reflection), not just speed.
- Privacy concerns: defaults and opt-outs must be clear and honored.
- Cheating via repeated trivial tasks: weight XP and badges to discourage grinding (e.g., cap repeatable XP per day for the same exercise).

---

## 2. Graspable Math Action Validation (isValidAction)

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
  5. **Performance, rate-limiting, and observability:**
     - Enforce a bounded validation budget in `GraspableMathService.isValidAction()` (and any `MathExpressionComparator` adapter): cancel or return `null` if parsing/comparison exceeds ~50 ms so that slow expressions do not block the UI.
     - Add a simple LRU cache keyed by `(expressionBefore, expressionAfter)` to short-circuit repeated checks and reduce redundant computation.
     - Make validation asynchronous/non-blocking where possible so UI threads are not held; consider returning a `CompletableFuture<Boolean>` or offloading to a worker thread if the framework permits.
     - Add per-user rate limiting/throttling on the validation entrypoint to prevent abuse or accidental denial-of-service from rapid actions.
     - Emit metrics (P50/P95/P99 latency, cache hit rate, timeout count) and structured logs for timeouts. Use these metrics to enable a log-only rollout before changing `correctActions` behavior in production.

- Backend changes (data/metrics):
  1. Ensure `StudentSessionEntity` handling in `GraspableMathService.processEvent()` handles `null`/`false` properly (do not increment correctActions for `false` or `null` if policy dictates).
  2. Add config toggles or feature flags (admin-settable) to control strictness: strict (treat unknown as incorrect), lenient (treat unknown as correct), or rollout mode (log only).

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

---

## 3. Multiple Problems Per Exercise with Sequential Unlocking

**Goal:** Allow exercises to have multiple problems (like hints), unlock "Next Problem" button when current is complete.

**Implementation Plan:**

### 3.1 Backend Changes

1. **ExerciseEntity/ExerciseViewDto** - Modify fields:
   - `graspableInitialExpression` -> Keep as is (semicolon-separated: "2x+5=15;3x-7=20;x^2=9")
   - `graspableTargetExpression` -> Add this field (semicolon-separated: "x=5;x=9;x=3")
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

### 3.2 Frontend Changes

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
   - Example: "2x+5=15;3x-7=20" -> Two problems in sequence

---

## 4. Vaadin Views — End-to-End Testing (Long-term)

**Package:** `de.vptr.aimathtutor.view`
**Gap:** Views are completely untested. Vaadin's UI thread model makes standard JUnit testing infeasible without a browser harness.
**Approach:** Introduce [Vaadin TestBench](https://vaadin.com/docs/latest/testing/end-to-end) or [Playwright](https://playwright.dev/) for end-to-end tests. This is a multi-day investment and should be treated as a separate project initiative, not a quick fix.

---

## 5. Encrypt-at-Rest

**Goal:** encrypt PII at rest with file-backed key management.

**Plan:**

1. Classify sensitive fields across entities (starting with `UserEntity.email`) and record lookup requirements (display-only vs searchable).
2. Implement field encryption using AES-256-GCM with random IV per value and versioned ciphertext envelope.
3. Add blind-index/hash companion columns for fields requiring equality search.
4. Introduce key management via `AIMATHTUTOR_ENCRYPTION_KEY_FILE`:
   - load key from file on startup
   - generate and persist key if missing
   - enforce restrictive file permissions
5. Add shared crypto service + entity converters/mappers so encryption/decryption is centralized and plaintext is never logged.
6. Add migrations to create encrypted/blind-index columns, backfill legacy plaintext, and remove plaintext columns after cutover.
7. Update `application.properties` with sensible local default key path and `docker-compose.yml` with env var + persistent volume mount.
8. Add unit/integration tests for crypto behavior, startup key generation, CRUD, and searchable encrypted fields.

**Done when:**

- Target PII fields are stored encrypted at rest.
- App starts with existing key or generates one when absent.
- Compose/dev setup persists key material via mounted volume.
- SpotBugs, Checkstyle, PMD, PMD-CPD and all Maven Tests passing.

---

## 6. Keyboard accessibility

**Problem:** 5 clickable `Span` components in admin Grid columns lack keyboard accessibility (not focusable, no Enter/Space handling, no ARIA role).

**Affected files (all in `view/admin/`):**

| File                       | Line | Variable       | Handler                         |
| -------------------------- | ---- | -------------- | ------------------------------- |
| `AdminExercisesView.java`  | 213  | `titleSpan`    | `openExerciseDialog`            |
| `AdminUserRanksView.java`  | 150  | `nameSpan`     | `openRankDialog`                |
| `AdminUsersView.java`      | 157  | `usernameSpan` | `openUserDialog`                |
| `AdminUserGroupsView.java` | 172  | `nameSpan`     | `openGroupDialog`               |
| `AdminSessionsView.java`   | 89   | `usernameSpan` | `UI.getCurrent().navigate(...)` |

All 5 follow an identical pattern: `new Span(text)` + style block (`color`, `cursor`, `width`, `display`) + `addClickListener(...)`.

**Fix per file:** Replace the Span with Vaadin `Button` using `theme="tertiary-inline"` (natively focusable, responds to Enter/Space, announced by screen readers, styled like a link):

```java
// Before:
final var titleSpan = new Span(exercise.title);
titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
titleSpan.getStyle().set("cursor", "pointer");
titleSpan.getStyle().set("width", "100%");
titleSpan.getStyle().set("display", "block");
titleSpan.addClickListener(ignored -> this.openExerciseDialog(exercise));
return titleSpan;

// After:
final var titleButton = new Button(exercise.title, ignored -> this.openExerciseDialog(exercise));
titleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
titleButton.getStyle().set("width", "100%");
return titleButton;
```

Note: `color` and `cursor` styles are removed — `LUMO_TERTIARY_INLINE` already uses primary text color and pointer cursor by default.

**Import changes per file:**

| File                       | Remove import | Add import                | Reason                                           |
| -------------------------- | ------------- | ------------------------- | ------------------------------------------------ |
| `AdminExercisesView.java`  | `Span`        | —                         | No longer used                                   |
| `AdminUserRanksView.java`  | —             | —                         | `Span` still used at line 229 (icon placeholder) |
| `AdminUsersView.java`      | —             | —                         | `Span` still used at line 245 (password label)   |
| `AdminUserGroupsView.java` | `Span`        | —                         | No longer used                                   |
| `AdminSessionsView.java`   | `Span`        | `Button`, `ButtonVariant` | No longer used, needs both                       |

All files except `AdminSessionsView.java` already import `Button` and `ButtonVariant`.

> **Consistency check:** All clickable-span patterns are in admin views only (confirmed via full codebase audit — no student views have clickable spans).

---

## 7. Test Coverage Improvements

### 7.1 Service classes with low coverage

| Class               | Lines Missed | Branches Missed | Notes                                                                          |
| ------------------- | ------------ | --------------- | ------------------------------------------------------------------------------ |
| `AnalyticsService`  | 113          | 52              | Depends on many repositories; hard to unit-test without extensive mocking      |
| `AuthService`       | 70           | 57              | Security-critical; needs careful `VaadinSession` mocking per anti-pattern note |
| `OpenAiService`     | 65           | 31              | REST client; needs `@QuarkusTest` with WireMock                                |
| `CommentService`    | 64           | 48              | Large service with many repository dependencies                                |
| `CommentRepository` | 55           | 18              | Integration test; currently only partial IT coverage                           |
| `AiTutorService`    | 53           | 60              | Complex orchestration service; needs mock providers                            |
| `OllamaService`     | 52           | 27              | REST client; same approach as OpenAiService                                    |
| `GeminiService`     | 42           | 8               | REST client; same approach as OpenAiService                                    |
| `AiConfigService`   | 38           | 52              | Large config service; existing tests but 38 lines still uncovered              |
| `UserService`       | 33           | 64              | Many branches; password/username validation paths                              |

The goal is to get line and branch coverage for the service package and it's subpackages as close to 100% as reasonably possible.

### 7.2 Repository integration tests needed

| Repository                | Lines Missed | Existing IT? |
| ------------------------- | ------------ | ------------ |
| `UserRepository`          | 22           | No           |
| `CommentFlagRepository`   | 23           | No           |
| `UserGroupMetaRepository` | 27           | No           |
| `AiConfigRepository`      | 9            | No           |
| `UserGroupRepository`     | 9            | No           |

The goal is to get line and branch coverage for repository and entity packages as close to 100% as reasonably possible.

---
