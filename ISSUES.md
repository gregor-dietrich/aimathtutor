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

## 7. Admin Dashboard Enhancement

**Goal:** Transform the current 4-stat-card dashboard into a comprehensive, professionally designed analytics dashboard suitable for a VC-backed EdTech startup, using **only already-available data sources** (no schema/entity changes).

**Current state:** 4 stat cards (Total Sessions, Completed, Active Students 7d, Today's Sessions) — purely textual, no visualizations, no trends, no drill-down. Current file: `AdminDashboardView.java` (123 lines).

**Target state:** Multi-section dashboard with KPI cards (with trend indicators), SVG time-series charts, distribution visualizations, top-performers list, and recent activity feed — all styled with Lumo design tokens, no external charting dependencies.

**Priority:** High — first thing admins see; sets tone for entire admin experience.
**Estimated difficulty:** ★★★☆☆ (primarily UI work with thin backend convenience methods)

---

### 7.1 Charting Strategy

**Approach:** Pure SVG charts generated server-side in a utility class. No additional dependencies.

**Rationale:**

- Zero new Maven dependencies (Vaadin Charts is commercial; JFreeChart would add weight)
- Works within Vaadin Flow's server-side rendering model
- SVG produces crisp, professional charts at any resolution
- Lumo CSS variables can be embedded in SVG for theme consistency
- Implementation is self-contained in ~3 files

**Reusable components to create:**

- `ChartUtil` — static methods returning SVG strings for line, bar, and donut charts
- `DashboardKpiCard` — styled card with value, label, trend arrow (% change)
- `ChartCard` — card wrapper with title + chart content
- `SVGComponent` — thin Vaadin component wrapping an SVG string in an `HtmlContainer`

---

### 7.2 Dashboard Layout

```txt
Row 1: KPI Cards (7 compact cards, responsive wrap)
  ┌─────────────┐ ┌──────────────┐ ┌───────────────┐ ┌──────────────┐
  │ Total        │ │ Completed    │ │ Active (7d)   │ │ Today        │
  │ Sessions     │ │ Sessions     │ │ Students      │ │ Sessions     │
  │ 1,234 ▲ 12%  │ │ 890 ▲ 8%     │ │ 156 ▼ 3%      │ │ 42 ▲ 25%     │
  └─────────────┘ └──────────────┘ └───────────────┘ └──────────────┘
  ┌─────────────┐ ┌──────────────┐ ┌───────────────┐
  │ Total       │ │ Published    │ │ Avg Success   │
  │ Users       │ │ Exercises    │ │ Rate          │
  │ 89 ▲ 5%     │ │ 45 — 0%      │ │ 72.3% ▲ 2%    │
  └─────────────┘ └──────────────┘ └───────────────┘

Row 2: Time-Series + Top Categories (2 equal columns)
  ┌──────────────────────────────┐ ┌──────────────────────────────┐
  │  Sessions per Day (30 days)  │ │  Top Exercises by Completion │
  │  [SVG line chart]            │ │  [SVG horizontal bar chart]  │
  │                              │ │                              │
  └──────────────────────────────┘ └──────────────────────────────┘

Row 3: Distributions (2 equal columns)
  ┌──────────────────────────────┐ ┌──────────────────────────────┐
  │  Completion Rate Distribution│ │  Hints Usage Distribution    │
  │  [SVG donut/bar chart]       │ │  [SVG horizontal bar chart]  │
  └──────────────────────────────┘ └──────────────────────────────┘

Row 4: Activity Feed + Top Performers (2 equal columns)
  ┌──────────────────────────────┐ ┌──────────────────────────────┐
  │  Recent Activity (7 days)    │ │  Top Students by Completion  │
  │  • Student A — Ex. 1 — 3m ago│ │  1. Student A — 45 sessions  │
  │  • Student B — Ex. 2 — 15m ago│ │  2. Student B — 38 sessions  │
  │  • Student C — Ex. 3 — 1h ago│ │  3. Student C — 32 sessions  │
  └──────────────────────────────┘ └──────────────────────────────┘
```

---

### 7.3 Metrics, Data Sources, and Computation

All sourced from existing `AnalyticsService`, `UserRepository`, `ExerciseRepository` — no new DB columns.

| #   | Metric                       | Data Source                                             | Computation                                                   |
| --- | ---------------------------- | ------------------------------------------------------- | ------------------------------------------------------------- |
| 1   | Total Sessions               | `AnalyticsService.getTotalSessionsCount()`              | Existing `countAll()`                                         |
| 2   | Completed Sessions           | `AnalyticsService.getCompletedSessionsCount()`          | Existing `countByCompleted(true)`                             |
| 3   | Active Students (7d)         | `AnalyticsService.getActiveStudentsCount()`             | Existing `countActiveStudentsSince()`                         |
| 4   | Today's Sessions             | `AnalyticsService.getTodaySessionsCount()`              | Existing half-open range count                                |
| 5   | Total Users                  | `AnalyticsService.getUserCount()`                       | **New:** `UserRepository` injected, `SELECT COUNT(u)` JPQL    |
| 6   | Published Exercises          | `AnalyticsService.getPublishedExerciseCount()`          | **New:** `ExerciseRepository.findPublished().size()`          |
| 7   | Avg Success Rate             | `AnalyticsService.getAllUsersProgressSummary()`         | Average of all `successRate` fields                           |
| 8   | Sessions per Day (30d)       | `AnalyticsService.getSessionsByDateRange(30dAgo, now)`  | Bucket by `startTime.toLocalDate()`, fill missing days with 0 |
| 9   | Top Exercises                | `AnalyticsService.getProblemCategoryStats()`            | Existing — sort by count, take top N                          |
| 10  | Completion Rate Dist.        | `AnalyticsService.getAllUsersProgressSummary()`         | Bucket `completedSessions/totalSessions` into 5 ranges        |
| 11  | Hints Usage Dist.            | `AnalyticsService.getAllUsersProgressSummary()`         | Sum all `hintsUsed` per student; bucket: 0, 1-5, 6-20, 21+    |
| 12  | Recent Sessions (7d)         | `AnalyticsService.getSessionsByDateRange(7dAgo, now)`   | Take first 10; show username, exercise, relative time         |
| 13  | Top Students (by completion) | `AnalyticsService.getAllUsersProgressSummary()`         | Sort by `completedSessions` desc, take 5                      |
| 14  | Trend % (vs prior period)    | `getSessionsByDateRange()` for current and prior period | `(current - prior) / prior * 100` for each KPI                |

**Trend computation:** For each KPI, compare current period (e.g., last 7 days) to the immediately preceding period (e.g., 7-14 days ago). This requires two date-range queries per KPI — acceptable since all are lightweight `count` queries.

---

### 7.4 New Backend Methods

Add to `AnalyticsService`. These are thin convenience wrappers — no new queries beyond what repositories already expose, all computation happens in Java after fetching data.

#### 7.4.1 `getUserCount()` → `long`

Delegates to `UserRepository` (currently not injected in `AnalyticsService` — add `@Inject UserRepository userRepository` field, which already exists at line 46).

Wait — `UserRepository` is already injected in `AnalyticsService` (line 46). Just need to call `this.userRepository.findAll().size()` or better, add a `countAll()` to `UserRepository`:

```java
// In UserRepository — new method:
public long countAll() {
    final var q = this.em.createQuery("SELECT COUNT(u) FROM UserEntity u", Long.class);
    return q.getSingleResult();
}
```

#### 7.4.2 `getPublishedExerciseCount()` → `long`

```java
// In AnalyticsService:
@Inject transient ExerciseRepository exerciseRepository;  // new injection
...
public long getPublishedExerciseCount() {
    return this.exerciseRepository.findPublished().size();
}
```

Or add a `countPublished()` to `ExerciseRepository` for efficiency.

#### 7.4.3 `getDailySessionCounts(int days)` → `LinkedHashMap<LocalDate, Long>`

```java
public LinkedHashMap<LocalDate, Long> getDailySessionCounts(int days) {
    var end = LocalDateTime.now();
    var start = end.minusDays(days);
    var sessions = getSessionsByDateRange(start, end);
    var counts = sessions.stream()
        .collect(Collectors.groupingBy(
            s -> s.startTime.toLocalDate(),
            LinkedHashMap::new,
            Collectors.counting()
        ));
    // Fill missing days with 0
    for (int i = days; i >= 0; i--) {
        counts.putIfAbsent(LocalDate.now().minusDays(i), 0L);
    }
    return counts;
}
```

#### 7.4.4 `getTrendData()` → `DashboardTrendDto`

DTO bundling 7-day and prior-7-day counts for each KPI, plus computed percentage changes. Methods needed:

- `countByStartTimeBetween(start, end)` already exists on `StudentSessionRepository`
- `countActiveStudentsSince(time)` already exists

New DTO:

```java
public class DashboardTrendDto {
    public final long totalSessions, prevTotalSessions;
    public final long completedSessions, prevCompletedSessions;
    public final long activeStudents, prevActiveStudents;
    public final long todaySessions, prevTodaySessions;
    // computed
    public double totalSessionsChange() { ... }
    public double completedSessionsChange() { ... }
    // etc.
}
```

#### 7.4.5 `getCompletionRateHistogram()` → `LinkedHashMap<String, Integer>`

Buckets: `0%`, `1–25%`, `26–50%`, `51–75%`, `76–99%`, `100%`.
Compute from `getAllUsersProgressSummary()` — for each student, compute `completedSessions / totalSessions`, map to bucket.

#### 7.4.6 `getHintUsageBuckets()` → `LinkedHashMap<String, Integer>`

Buckets: `0 hints`, `1–5 hints`, `6–20 hints`, `21+ hints`.
Sum hintsUsed from `getAllUsersProgressSummary()`, bucket.

#### 7.4.7 `getRecentSessions(int limit)` → `List<StudentSessionViewDto>`

```java
public List<StudentSessionViewDto> getRecentSessions(int limit) {
    var weekAgo = LocalDateTime.now().minusDays(7);
    var sessions = getSessionsByDateRange(weekAgo, null);
    return sessions.stream().limit(limit).toList();
}
```

#### 7.4.8 `getTopStudentsByCompletion(int limit)` → `List<StudentProgressSummaryDto>`

```java
public List<StudentProgressSummaryDto> getTopStudentsByCompletion(int limit) {
    return getAllUsersProgressSummary().stream()
        .sorted(Comparator.comparingInt(s -> s.completedSessions != null ? s.completedSessions : 0))
        .limit(limit)
        .toList();
}
```

#### 7.4.9 DTO: `DashboardData` (bundled result)

Inner class or record in `AdminDashboardView` to pass all data in a single async load:

```java
record DashboardData(
    long totalSessions, long completedSessions, long activeStudents, long todaySessions,
    long totalUsers, long publishedExercises, double avgSuccessRate,
    LinkedHashMap<LocalDate, Long> dailySessionCounts,
    LinkedHashMap<String, Integer> topExercises,
    LinkedHashMap<String, Integer> completionRateHistogram,
    LinkedHashMap<String, Integer> hintUsageBuckets,
    List<StudentSessionViewDto> recentSessions,
    List<StudentProgressSummaryDto> topStudents,
    DashboardTrendDto trends
) {}
```

---

### 7.5 New UI Components

Package: `de.vptr.aimathtutor.component.dashboard` (new sub-package)

| File                      | Type             | Description                                                                      |
| ------------------------- | ---------------- | -------------------------------------------------------------------------------- |
| `DashboardKpiCard.java`   | `VerticalLayout` | Single KPI: value (large bold), label (uppercase small), trend arrow+% (colored) |
| `ChartCard.java`          | `VerticalLayout` | Wrapper: title + chart content with consistent card styling                      |
| `SvgLineChart.java`       | `HtmlContainer`  | Renders an SVG `<svg>` element line chart from data points                       |
| `SvgBarChart.java`        | `HtmlContainer`  | Renders SVG horizontal bar chart from labeled values                             |
| `SvgDonutChart.java`      | `HtmlContainer`  | Renders SVG donut chart from category-value pairs                                |
| `RecentActivityFeed.java` | `VerticalLayout` | Styled list of recent sessions with relative timestamps                          |
| `TopStudentsList.java`    | `VerticalLayout` | Compact ranked list of top students                                              |

#### 7.5.1 `DashboardKpiCard` Design

```java
┌─────────────────────┐
│                     │
│   1,234             │  ← value (--lumo-primary-color, 32px, 800 weight)
│                     │
│   TOTAL SESSIONS    │  ← label (--lumo-secondary-text-color, 11px, uppercase, 600 tracking)
│                     │
│   ↑ 12.3% vs last  │  ← trend (--lumo-success-color for up, --lumo-error-color for down)
│       7 days       │
└─────────────────────┘
```

- Fixed width: ~200px or flex-grow equal
- Border radius: 8px
- Background: `var(--lumo-base-color)` with `1px solid var(--lumo-contrast-10pct)`
- Hover: subtle `box-shadow` elevation

#### 7.5.2 SVG Chart Specifications

All charts use `viewBox` for responsiveness and embed Lumo CSS variable colors via inline `<style>` within SVG.

**Line Chart (Sessions per Day):**

- 800×300 viewBox
- Light gray gridlines (horizontal only, 4-5 lines)
- X-axis: day labels (every 5th day)
- Y-axis: session count (3-4 tick marks)
- Line: `--lumo-primary-color` (2px stroke), with gradient fill below
- Dots on data points (4px radius, same color)
- No legend (single series)

**Horizontal Bar Chart (Top Exercises / Hint Buckets):**

- 600×250 viewBox (height scales with item count)
- Bar: `--lumo-primary-color` with 80% opacity
- Label left-aligned, value right-aligned
- Bar length proportional to max value

**Donut Chart (Completion Rate):**

- 300×300 viewBox
- 6 segments with distinct hues from Lumo palette
- Center text: "Completion Rate" + average percentage
- Segments ordered clockwise from "0%" at top

#### 7.5.3 ChartUtil SVG Generation

```java
public final class ChartUtil {
    public static String lineChart(
        LinkedHashMap<LocalDate, Long> data,
        String title, int width, int height
    ) { ... }

    public static String horizontalBarChart(
        LinkedHashMap<String, Integer> data,
        String title, int width, int height
    ) { ... }

    public static String donutChart(
        LinkedHashMap<String, Integer> data,
        String centerLabel, String centerValue, int size
    ) { ... }
}
```

Returns a complete `<svg>` element string embeddable in a Vaadin `HtmlContainer`.

---

### 7.6 Rebuild AdminDashboardView

Current file: 123 lines → target: ~300-400 lines (including inner `DashboardData` record).

**Structure:**

```java
@Route(value = "admin/dashboard", layout = AdminMainLayout.class)
@PageTitle("Admin Dashboard - AI Math Tutor")
public class AdminDashboardView extends AbstractAdminView {

    @Inject private transient AnalyticsService analyticsService;

    // UI component references for partial updates
    private transient List<DashboardKpiCard> kpiCards;
    private transient ChartCard sessionsChart;
    private transient ChartCard topExercisesChart;
    // ...

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!isAuthOk(event)) return;
        buildUi();
        loadDashboardData();
    }

    private void buildUi() {
        removeAll();
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Title
        add(new H2("Dashboard Overview"));

        // Row 1: KPI cards in HorizontalLayout with wrapping
        var kpiRow = new HorizontalLayout();
        kpiRow.setWidthFull();
        kpiRow.setFlexGrow(1, kpiCards...);
        add(kpiRow);

        // Row 2: Two ChartCards side by side
        var chartRow1 = new HorizontalLayout(...);
        add(chartRow1);

        // Row 3: Two ChartCards side by side
        var chartRow2 = new HorizontalLayout(...);
        add(chartRow2);

        // Row 4: Activity + Top Students
        var bottomRow = new HorizontalLayout(...);
        add(bottomRow);
    }

    private void loadDashboardData() {
        AsyncDataLoader.load(() -> {
            var totalSessions = analyticsService.getTotalSessionsCount();
            var completedSessions = analyticsService.getCompletedSessionsCount();
            // ... all other metrics ...
            var dailyCounts = analyticsService.getDailySessionCounts(30);
            var trends = analyticsService.getTrendData();
            // ...
            return new DashboardData(totalSessions, completedSessions, ..., dailyCounts, trends);
        }, this, this::renderDashboard, "Failed to load dashboard data");
    }

    private void renderDashboard(DashboardData data) {
        updateKpiCards(data);
        updateCharts(data);
        updateActivityFeed(data);
        updateTopStudents(data);
    }
}
```

---

### 7.7 Async Data Loading & Performance

- **Single async load**: One `CompletableFuture.supplyAsync()` call fetches ALL dashboard data bundled in `DashboardData` — avoids multiple sequential network round-trips.
- **Lightweight queries**: Each KPI uses a simple `COUNT` query (existing); time-series and distribution data query sessions with date ranges, bucketed in Java.
- **Acceptable latency**: Even with 50K sessions, bucketing by date in Java completes in <50ms. The `getAllUsersProgressSummary()` method already demonstrates this approach.
- **Caching**: Not needed for MVP — queries are fast. Future: add a 60-second in-memory cache on `DashboardData` if latency becomes an issue.
- **Empty state**: All charts/cards gracefully handle empty data (show "No data" or zeroes).

---

### 7.8 Startup Dashboard Visual Design Principles

| Principle                | Implementation                                                     |
| ------------------------ | ------------------------------------------------------------------ |
| **Minimal ink** (Tufte)  | Light gray gridlines, no chart borders, minimal axis labels        |
| **Data-ink ratio**       | No 3D, no gradients, no extraneous decorations                     |
| **Color meaning**        | Lumo success (↑ positive), error (↓ negative), primary (main data) |
| **Consistent spacing**   | 24px between sections, 16px between cards, 12px within cards       |
| **Typography hierarchy** | 32px KPI values → 14px secondary text → 11px labels                |
| **Interactive feedback** | Subtle shadow elevation on hover for cards                         |
| **Loading state**        | Cards show "—" or skeleton placeholder while data loads            |
| **Error state**          | Cards show "⚠" with error; user can refresh                        |

---

### 7.9 Implementation Order (Recommended)

| Step | What                                                                                                                                     | Creates                         | Est. effort |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- | ----------- |
| 1    | Add `countAll()` to `UserRepository`; `getUserCount()`, `getPublishedExerciseCount()` to `AnalyticsService`                              | 2 small methods                 | 15 min      |
| 2    | Add `getDailySessionCounts()`, `getTrendData()` to `AnalyticsService`                                                                    | 2 methods + `DashboardTrendDto` | 30 min      |
| 3    | Add `getCompletionRateHistogram()`, `getHintUsageBuckets()`, `getRecentSessions()`, `getTopStudentsByCompletion()` to `AnalyticsService` | 4 methods                       | 30 min      |
| 4    | Create `ChartUtil` with SVG generation methods                                                                                           | 1 utility + 3 SVG generators    | 2-3 h       |
| 5    | Create `DashboardKpiCard`, `ChartCard` components                                                                                        | 2 component classes             | 1 h         |
| 6    | Create `SvgLineChart`, `SvgBarChart`, `SvgDonutChart` thin wrappers                                                                      | 3 wrapper classes               | 30 min      |
| 7    | Create `RecentActivityFeed`, `TopStudentsList`                                                                                           | 2 component classes             | 30 min      |
| 8    | Rebuild `AdminDashboardView` with new layout + async loading                                                                             | Rewrite dashboard view          | 2 h         |
| 9    | Manual testing + visual polish                                                                                                           | —                               | 1 h         |
| 10   | Run `make lint` + `make test`                                                                                                            | —                               | 5 min       |

**Total estimated effort: ~8-10 hours** (one focused day)

---

### 7.10 Testing

- **Unit tests** for:
  - `ChartUtil` SVG output structure (valid XML, correct viewBox, presence of data points)
  - `DashboardTrendDto` percentage change computation (positive, negative, zero, divide-by-zero)
  - New `AnalyticsService` methods (daily counts, histograms, top N)
  - Empty data edge cases
- **Manual verification**:
  - All 4 layout rows render correctly side by side
  - SVG charts display with proper colors and scaling
  - Trend arrows show correct direction and color
  - Responsive wrap on narrow window
  - Loading and error states display correctly
- **Existing tests must pass**: `make test` and `make lint`

---

### 7.11 Future Enhancements (Out of Scope)

- Time-period selector (7d/30d/90d/1y toggle) — requires component state management
- Interactive chart tooltips — requires Chart.js or JS integration
- Dashboard PDF/PNG export — requires HTML2Canvas or Puppeteer
- Drag-and-drop dashboard widget layout — requires Vaadin DragSource/DropTarget
- Click-to-drill-down (chart click navigates to filtered admin view) — requires chart click handlers in SVG
- Automated email report delivery — requires scheduler + template engine
- Export to CSV for specific chart data
- Custom date range picker for chart data

---
