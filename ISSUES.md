# TODO - Detailed Implementation Plans

## 0. General

### Implementation Priority

1. **Security & Safety** (Auth unification, XSS, JS injection, rate limits, password storage, API key exposure)
2. **Error Handling & Reliability** (Broad exception catches, NPE risks, PII in logs, user-facing error leaks)
3. **Database & Query Optimization** (Broken named queries, missing indexes, N+1 queries, in-memory filtering)
4. **Graspable Math Action Validation (isValidAction)** (Task 7)
5. **Multiple Problems Per Exercise** (Task 2)
6. **AdminConfigView: Performance Optimization** (Task 5.2 — service-level config caching)
7. **Gamification** (Task 6)
8. **Code Quality & Architecture** (Base admin view, split oversized services, extract constants, dead code)
9. **AI Service Hardening** (Prompt injection defense, config validation, response handling)
10. **Miscellaneous Fixes** (Keyboard accessibility, admin dashboard diagrams)
11. **Pagination** (Phase 7 — lowest priority)

### Testing Checklist (for each feature)

- [ ] Unit tests for service methods
- [ ] Integration tests for DB operations
- [ ] Manual UI testing in both views
- [ ] Edge cases (empty data, invalid input, etc.)
- [ ] Permission/security checks
- [ ] Performance with large datasets (admin views)

---

## 1. Graspable Math Action Validation (isValidAction)

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

## 2. Multiple Problems Per Exercise with Sequential Unlocking

**Goal:** Allow exercises to have multiple problems (like hints), unlock "Next Problem" button when current is complete.

**Implementation Plan:**

### 2.1 Backend Changes

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
   - Example: "2x+5=15;3x-7=20" -> Two problems in sequence

---

## 3. Gamification

**Goal:** Increase student motivation and engagement by adding gamification elements such as achievements/badges, progress levels, experience points (XP), streaks, leaderboards, and rewards tied to problem solving within the Graspable Math workspace and overall course progress.

This feature should be opt-in per user (privacy-friendly), configurable by admins, and designed to be low-friction so it does not interfere with learning objectives.

### 3.1 High-level features

- Achievements/Badges: award for specific milestones (e.g., "First Solution", "10 Problems Solved", "Perfect Session", "Fast Solver", "Hint Avoider").
- Experience points (XP): reward XP for solved problems, streaks, and completing exercises. XP contributes to user Level.
- Levels & Progress Bar: users level up based on XP thresholds; show a progress bar on dashboard and exercise view.
- Daily Streaks: consecutive days with activity—rewards and streak badges.
- Leaderboards: global and class/group leaderboards showing top XP or most problems solved. Respect privacy settings (opt-in/opt-out, show anonymized handles).
- Challenges & Quests: time-limited or teacher-assigned challenges (e.g., "Solve 5 linear equations this week") with rewards.
- Rewards & Unlocks: unlock cosmetic rewards (avatars, themes), extra practice problems, or hints currency that can be spent.
- Notifications & Activity Feed: notify users when they earn badges, level up, or climb the leaderboard.

### 3.2 Backend changes

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

### 3.3 Frontend changes (Vaadin views)

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

### 3.4 XP, Levels, and Rules (example policy)

- Base XP per solved problem: 10 XP
- Bonus: +5 XP for solving without hints
- Speed bonus: up to +10 XP proportional to time under expected time
- Streak bonus: +2 XP per consecutive day active (capped)
- Challenge completion: rewardXp per challenge config
- Level thresholds: exponential or pre-configured table (e.g., Level 1: 0 XP, Level 2: 100 XP, Level 3: 300 XP, Level 4: 700 XP)

Keep rules configurable via `AdminGamificationView`.

### 3.5 Privacy & Accessibility

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

### 3.6 Testing

- Unit tests for `GamificationService` (award logic, XP calculations, level progression).
- Integration tests for DB writes (badge awards, XP updates, streak increments).
- UI tests for badge modal display and leaderboard filtering.
- Load testing/benchmarks for leaderboard queries (cache snapshots if needed).

### 3.7 Metrics & Analytics

- Track gamification engagement metrics: percent of users opting in, average XP earned per session, badge earn rates, churn/retention impact.
- Add events to existing logging/analytics pipeline (e.g., `GAMIFICATION_BADGE_AWARDED`, `GAMIFICATION_XP_ADDED`).

### 3.8 Phased rollout and migration

- Phase 1 (MVP): XP, badges for a small default set (First Solution, 10 Problems, No Hints), user opt-in, basic UI panel, and admin config for enabling/disabling.
- Phase 2: Add leaderboards, challenges, rewards/unlocks, and teacher tools.
- Phase 3: Advanced features like seasonal events, classroom competitions, and integration with external LMS.

### 3.9 Risks and mitigations

- Reward focus over learning: design badges to align tightly with learning goals (e.g., accuracy, explanation, reflection), not just speed.
- Privacy concerns: defaults and opt-outs must be clear and honored.
- Cheating via repeated trivial tasks: weight XP and badges to discourage grinding (e.g., cap repeatable XP per day for the same exercise).

---

## 4. Miscellaneous Fixes

### 4.1 Admin Dashboard Enhancement

The admin dashboard could use some further enhancement, such as diagrams.

### 4.2 Keyboard accessibility

Clickable spans are used extensively across views, especially admin views, however they lack keyboard accessibility. Users navigating with keyboards cannot trigger the click event. Consider using a Button or Anchor component with appropriate ARIA attributes, or add keyboard event listeners (Enter/Space) to the Span.

> **Consistency check:** When fixing, check all views (admin and student) for identical clickable-span patterns and fix them consistently.

### 4.3 Pagination

Add server-side pagination for admin views to handle large datasets gracefully (Vaadin `DataProvider.fromCallbacks()` + backend paged queries + `count()` methods). **Priority: Phase 7 — lowest urgency.**

> **Consistency check:** When implementing, check all admin views (Users, Exercises, Lessons, Comments, Sessions, Progress, UserGroups, UserRanks) for identical grid-loading patterns and fix them consistently.

### 4.4 Unit Test Coverage

Unit test coverage should be reviewed and improved across multiple packages. Specific gaps identified:

- Service tests only cover null/empty input validation (no happy paths, updates, deletions, search, or edge cases).
- No repository tests for custom queries (e.g., broken named queries in `ExerciseRepository` would not be caught).
- No view/presenter tests; Vaadin views are completely untested.
- No usage of Mockito/Panache Mock for true unit tests in isolation; all tests use `@QuarkusTest` with real DB.
- `AiTutorServiceTest` has a test that catches generic `Exception` and asserts on message, making it pass regardless of actual behavior.

> **Consistency check:** When adding tests, check all service test classes for identical gaps and fix them consistently.

### 4.5 ULIDs as Primary/Foreign Keys (API)

**Goal:** remove sequential IDs from external surfaces; use ULIDs to reduce enumeration risk.

**Plan:**

1. Inventory every entity/resource exposing numeric IDs in API routes, DTOs, and GUI URLs.
2. Add immutable `publicId` ULID column per entity (unique + indexed). Keep current numeric PK/FK internal during migration.
3. Create DB migration to add columns and indexes, backfill ULIDs for existing rows, then enforce `NOT NULL`.
4. Update API contracts to accept/return ULIDs only (path params, request/response DTOs, service lookups by `publicId`).
5. Update GUI routing and REST clients to use ULIDs end-to-end.
6. Add ULID validation + tests for CRUD and authorization flows using ULID identifiers.
7. Roll out in two steps: compatibility window (dual-read), then remove numeric-ID access from controllers/DTOs.

**Done when:**

- No external endpoint exposes numeric IDs.
- All existing records have unique non-null ULIDs.
- Clients and docs fully switched to ULIDs.
- SpotBugs, CheckStyle and all Maven Tests passing for api and gui.

### 4.6 Encrypt-at-Rest (API)

**Goal:** encrypt PII at rest with file-backed key management.

**Plan:**

1. Classify sensitive fields across entities (starting with `UserEntity.email`, `UserEntity.lastIp`) and record lookup requirements (display-only vs searchable).
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
- SpotBugs, CheckStyle and all Maven Tests passing for api and gui.

### 4.7 Vaadin 25 theme migration fixes (GUI)

- Replace deprecated `lumoImports` usage in `src/main/frontend/themes/starter-theme/theme.json`.
- If utility classes required, add `@StyleSheet(Lumo.UTILITY_STYLESHEET)` in `src/main/java/de/vptr/aimathtutor/gui/AppConfig.java`.
- Fix component-style loading warning (`vaadin-text-field.css`) by enabling `themeComponentStyles` or moving styles to supported setup.

---

## 5. Remediation Plan — Code Review Findings (3.0.0)

---

### Phase 1: Security (Critical)

#### ~~1.1 Switch password hashing from PBKDF2 to bcrypt~~ ✅
- **Problem:** `PasswordHashingService` uses PBKDF2-HMAC-SHA256 with 100,000 iterations, which is below current OWASP recommendations and dated compared to bcrypt.
- **Where:** `src/main/java/de/vptr/aimathtutor/security/PasswordHashingService.java`
- **Fix:** Replace PBKDF2 implementation with `io.quarkus.elytron.security.common.BcryptUtil.bcryptHash()` and `BcryptUtil.matches()`. Remove salt generation since bcrypt embeds the salt in the hash string.
- **Also updated:**
  - `src/main/java/de/vptr/aimathtutor/service/AuthService.java:93`
  - `src/main/java/de/vptr/aimathtutor/security/UserIdentityProvider.java:67`
  - `src/main/java/de/vptr/aimathtutor/util/PasswordUtility.java:48-49`
  - `src/test/java/de/vptr/aimathtutor/security/PasswordHashingServiceTest.java`
  - `src/test/java/de/vptr/aimathtutor/util/PasswordUtilityTest.java`
  - `pom.xml`

#### ~~1.2 Fix timing attack in password comparison~~ ✅
- **Problem:** `PasswordHashingService.verifyPassword` previously used `String.equals()` which short-circuits on mismatch, enabling timing attacks.
- **Where:** `src/main/java/de/vptr/aimathtutor/security/PasswordHashingService.java:91`
- **Fix:** `BcryptUtil.matches()` already uses constant-time comparison internally, so switching to bcrypt (task 1.1) resolves this automatically. Verify no `String.equals()` remains in the final implementation.

#### ~~1.3 Fix brute-force bypass in UserIdentityProvider~~ ✅
- **Problem:** `UserIdentityProvider` authenticates users for Quarkus security but does **not** integrate with `LoginAttemptService`. An attacker can bypass the application’s login-throttling by hitting Quarkus-secured endpoints directly.
- **Where:** `src/main/java/de/vptr/aimathtutor/security/UserIdentityProvider.java:54-96`
- **Fix:** Inject `LoginAttemptService` into `UserIdentityProvider`. Before verifying credentials, check `loginAttemptService.isLockedOut(username)` and throw `AuthenticationFailedException` if locked out. On failed verification, call `recordFailedAttempt(username)`.

#### ~~1.4 Fix username enumeration via timing in UserIdentityProvider~~ ✅
- **Problem:** `if (user == null || !this.passwordHashingService.verifyPassword(...))` short-circuits: for non-existent usernames the expensive verify call is skipped, revealing valid usernames via timing.
- **Where:** `src/main/java/de/vptr/aimathtutor/security/UserIdentityProvider.java:67`
- **Fix:** When `user == null`, perform a dummy `verifyPassword()` call against a static dummy bcrypt hash (e.g., a hardcoded invalid hash) before rejecting, so the code path always executes the expensive operation. Then always call `loginAttemptService.recordFailedAttempt(username)` regardless of whether the user exists.

#### ~~1.5 Fix inconsistent username normalization~~ ✅
- **Problem:** `AuthService.authenticate()` normalizes usernames to lowercase (`username.toLowerCase().trim()`), but `UserIdentityProvider.authenticate()` and `UserRankIdentityAugmentor.augment()` do not. If a user is stored as "Admin" but logs in as "admin", the identity provider may fail to find the user and assign no roles.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/service/AuthService.java:59`
  - `src/main/java/de/vptr/aimathtutor/security/UserIdentityProvider.java:57`
  - `src/main/java/de/vptr/aimathtutor/security/UserRankIdentityAugmentor.java:45`
- **Fix:** Apply identical normalization (`toLowerCase().trim()`) in both `UserIdentityProvider` and `UserRankIdentityAugmentor` before querying the database.

#### ~~1.6 Fix session fixation vulnerability~~ ✅
- **Problem:** `AuthService.logout()` clears session attributes but does not invalidate the underlying HTTP session. The session ID is reused, enabling session fixation attacks.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/AuthService.java:131-139`
- **Fix:** In `logout()`, call `VaadinSession.getCurrent().getSession().invalidate()` to destroy the HTTP session entirely. Optionally also call `VaadinSession.getCurrent().close()`.

#### ~~1.7 Add @JsonIgnore to sensitive UserEntity fields~~ ✅
- **Problem:** `password`, `salt`, and `activationKey` are not annotated with `@JsonIgnore`. Any future REST endpoint or accidental JSON serialization will leak password hashes and activation tokens.
- **Where:** `src/main/java/de/vptr/aimathtutor/entity/UserEntity.java:52-56,71-72`
- **Fix:** Add `@JsonIgnore` annotation to the `password`, `salt`, and `activationKey` fields.

#### ~~1.8 Protect unprotected CommentService overloads~~ ✅
- **Problem:** `CommentService.updateComment()`, `patchComment()`, and `deleteComment(Long)` are public service methods that perform updates/hard deletes without calling `CommentPermissionService`. Any future view or accidental direct invocation would allow unauthorized modification.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/CommentService.java:302-318,329-343,352-355`
- **Fix:** Change the visibility of these three methods from `public` to package-private (no access modifier) so they can only be called from within the `de.vptr.aimathtutor.service` package. The public API (`editComment`, `deleteComment(Long, Long, boolean)`) already enforces permissions and should remain the only external entry point.

#### ~~1.9 Sanitize comment content to prevent latent stored XSS~~ ✅
- **Problem:** Comment content is stored in the database without any sanitization. While Vaadin `Span` currently renders it as text nodes, any future REST endpoint or `innerHTML`-based rendering would be immediately vulnerable to stored XSS.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/CommentService.java:272,314,419`
- **Fix:** Add a private `sanitizeCommentContent(String)` method in `CommentService` that strips HTML tags using a simple regex (e.g., `content.replaceAll("<[^>]*>", "")`) and trims whitespace. Call this method in `createComment()` before persisting content. Do not add a new external dependency for this.

---

### Phase 2: Data Integrity & JPA (Critical / High)

#### ~~2.1 Fix FK violation on Exercise deletion~~ ✅
- **Problem:** `ExerciseEntity` owns a `@OneToMany(mappedBy = "exercise")` list of comments with no cascade and no `orphanRemoval`. `CommentEntity.exercise` has `@JoinColumn(nullable = false)`. Deleting an exercise that has comments triggers a foreign-key constraint violation.
- **Where:** `src/main/java/de/vptr/aimathtutor/entity/ExerciseEntity.java:76-78`
- **Fix:** Add `cascade = CascadeType.REMOVE, orphanRemoval = true` to `ExerciseEntity.comments`. When an exercise is deleted, all its comments are automatically deleted.

#### ~~2.2 Fix comment reply reparenting on deletion~~ ✅ (Intentional SET NULL)
- **Problem:** `CommentEntity` has a self-referencing `parentComment` with no cascade settings. Deleting a comment row causes the database to set child rows' `parent_comment_id` to NULL, turning replies into top-level comments.
- **Where:** `src/main/java/de/vptr/aimathtutor/entity/CommentEntity.java:79-81`
- **Fix:** SET NULL is the intended behavior. Replies become top-level comments on the same exercise. No JPA change needed.

#### ~~2.3 Fix User deletion orphaning content or failing~~ ✅
- **Problem:** `UserEntity` has `@OneToMany(mappedBy = "user")` for exercises and comments with no cascade. Deleting a user will nullify `user_id` on exercises/comments (orphaning them) or throw an FK violation for `UserGroupMetaEntity` (which has `nullable = false`).
- **Where:** `src/main/java/de/vptr/aimathtutor/entity/UserEntity.java:85-91`
- **Fix:** Added missing `@OneToMany` mappings on `UserEntity`:
  - `commentFlags` → `cascade = CascadeType.REMOVE, orphanRemoval = true`
  - `userGroupMetas` → `cascade = CascadeType.REMOVE, orphanRemoval = true`

#### ~~2.4 Fix UserGroup deletion failing if members exist~~ ✅
- **Problem:** `UserGroupEntity` has `@OneToMany(mappedBy = "group")` with no cascade. `UserGroupMetaEntity.group` is `nullable = false`. Deleting a non-empty group always throws an FK violation.
- **Where:** `src/main/java/de/vptr/aimathtutor/entity/UserGroupEntity.java:35-36`
- **Fix:** Added `cascade = CascadeType.REMOVE, orphanRemoval = true` to `UserGroupEntity.userGroupMetas`.

#### ~~2.5 Fix UserRank deletion failing if users exist~~ ✅ (Already implemented)
- **Problem:** `UserEntity.rank` has `@JoinColumn(nullable = false)`. Deleting a rank that has assigned users will throw an FK violation.
- **Where:** `src/main/java/de/vptr/aimathtutor/entity/UserEntity.java:58-61`
- **Fix:** `UserRankService.deleteRank()` already checks `userRepository.countByRankId(id)` and throws `WebApplicationException(CONFLICT)` if users exist.

#### ~~2.6 Fix Lesson deletion silently orphaning children~~ ✅ (Intentional SET NULL)
- **Problem:** `LessonEntity` has `@OneToMany(mappedBy = "parent")` and `@OneToMany(mappedBy = "lesson")` with no cascade. Both FK columns are nullable. Deleting a lesson promotes child lessons to root and unassigns exercises without warning.
- **Where:** `src/main/java/de/vptr/aimathtutor/entity/LessonEntity.java:46-50`
- **Fix:** SET NULL is the intended behavior. Child lessons become root lessons and exercises become unassigned. No JPA change needed.

#### ~~2.7 Align @Column(nullable=false) with @NotBlank/@NotNull~~ ✅
- **Problem:** Many fields marked with `@NotBlank` or `@NotNull` lack the matching `@Column(nullable = false)` or `@JoinColumn(nullable = false)`. Because production uses `hibernate.hbm2ddl.auto=validate`, mismatches between Java mappings and the actual schema will cause startup failures.
- **Where:** All entities
- **Fix:** Added `@Column(nullable = false)` to all required fields across `UserEntity`, `ExerciseEntity`, `CommentEntity`, `LessonEntity`, `UserGroupEntity`, `UserRankEntity`, `StudentSessionEntity`, `AiConfigEntity`, `AiInteractionEntity`, `CommentFlagEntity`.

#### ~~2.8 Standardize timestamp columns~~ ✅
- **Problem:** `timestamp` and `lastUpdatedAt` columns existed instead of uniform `created`/`last_edit`.
- **Where:** Multiple entities.
- **Fix:**
  - Renamed `AiConfigEntity.lastUpdatedAt` → `last_edit`
  - Renamed `AiInteractionEntity.timestamp` → `created`
  - Renamed `UserGroupMetaEntity.timestamp` → `created`
  - Removed `@PrePersist` from `AiInteractionEntity` and `StudentSessionEntity`
  - Updated all callers (`AiConfigService`, `UserGroupService`, `AiInteractionViewDto`, `AdminSessionView`)

#### ~~2.9 Add @Version to mutable entities~~ ✅
- **Problem:** Only `CommentEntity` has `@Version`. All other mutable entities lack optimistic locking, so concurrent updates silently overwrite each other (last-write-wins).
- **Where:** All entities except `CommentEntity`.
- **Fix:** Added `@Version Long version` to `UserEntity`, `ExerciseEntity`, `LessonEntity`, `UserGroupEntity`, `UserRankEntity`, `StudentSessionEntity`, `AiConfigEntity`, `AiInteractionEntity`, `CommentFlagEntity`.

#### ~~2.10 Fix comment flag creation race condition~~ ✅
- **Problem:** `CommentFlagRepository.createFlag()` checks `hasUserFlaggedComment()` before inserting, which is a classic check-then-act race. Two concurrent requests can pass the check simultaneously; the second hits the unique constraint and throws a low-level `PersistenceException`.
- **Where:** `src/main/java/de/vptr/aimathtutor/repository/CommentFlagRepository.java:58-74`
- **Fix:** Wrapped `persist()` + `flush()` in try/catch for `PersistenceException`. Translates unique constraint violation into the same friendly `WebApplicationException`.

#### ~~2.11 Fix UserGroupService.addUserToGroup race condition~~ ✅
- **Problem:** `addUserToGroup` checks `isUserInGroup()` before inserting, creating a check-then-act race for duplicate memberships.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/UserGroupService.java:208-228`
- **Fix:** Removed the explicit pre-check. Now persists directly and catches `PersistenceException` caused by the unique constraint, translating it into a user-friendly exception.

---

### Phase 3: Resource Leaks & Performance (Critical / High)

#### 3.1 Fix JAX-RS Response leaks
- **Problem:** `OllamaService` and `OpenAiService` obtain JAX-RS `Response` objects but never close them. `readEntity()` consumes the entity stream but does not close the underlying `Response`, eventually exhausting the connection pool.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/service/OllamaService.java:109,165,191`
  - `src/main/java/de/vptr/aimathtutor/service/OpenAiService.java:135,229`
- **Fix:** Wrap all JAX-RS calls in try-with-resources: `try (Response response = ...) { ... }`.

#### 3.2 Fix ConversationContextDto thread safety
- **Problem:** `ConversationContextDto` holds plain `ArrayList` instances (`recentActions`, `recentQuestions`, `recentAiMessages`). The view passes the same instance to `CompletableFuture.supplyAsync()` running on `ManagedExecutor`. The UI thread may concurrently mutate these lists while the background thread iterates over them, causing `ConcurrentModificationException`.
- **Where:** `src/main/java/de/vptr/aimathtutor/dto/ConversationContextDto.java:15,18,21`
- **Fix:** Change the list types to `CopyOnWriteArrayList` or create defensive copies before passing to background threads.

#### 3.3 Fix AdminConfigView async executor misuse
- **Problem:** Admin config view connection tests call `CompletableFuture.supplyAsync(testCall::get)` without an explicit `Executor`, using `ForkJoinPool.commonPool()`. Blocking HTTP calls occupy common-pool threads, and CDI contexts are not propagated.
- **Where:** `src/main/java/de/vptr/aimathtutor/view/admin/AdminConfigView.java:394`
- **Fix:** Inject `ManagedExecutor` and pass it to `supplyAsync`: `CompletableFuture.supplyAsync(testCall::get, this.managedExecutor)`.

#### 3.4 Fix GraspableMathService regex compilation on every call
- **Problem:** `normalizeExpression()` calls `expression.replaceAll("\\s+", "")` which compiles the regex pattern on every invocation.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/GraspableMathService.java:223`
- **Fix:** Use a static precompiled `Pattern`: `private static final Pattern WHITESPACE = Pattern.compile("\\s+");` and then `WHITESPACE.matcher(normalized).replaceAll("")`.

#### 3.5 Fix blocking DNS lookup inside @Transactional
- **Problem:** `AiConfigService.validateUrlSafe()` calls `InetAddress.getByName(host)` (blocking DNS) from within `@Transactional` methods. A slow DNS query holds a DB connection open, potentially exhausting the pool.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/AiConfigService.java:448-508`
- **Fix:** Move URL/SSRF validation outside the `@Transactional` boundary. Perform validation before entering the transactional method, or split the service method into a non-transactional validation phase and a transactional update phase.

#### 3.6 Fix unnecessary @Transactional on cache reads
- **Problem:** `AiConfigService` getters (including cache lookups) are annotated with `@Transactional`. Opening a JTA transaction just to read from a `ConcurrentHashMap` wastes resources and holds DB connection pool slots.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/AiConfigService.java` getters
- **Fix:** Remove `@Transactional` from pure cache read methods. Only annotate methods that actually interact with the repository.

---

### Phase 4: Error Handling & Null Safety (High / Medium)

#### 4.1 Fix CommentService.findByDateRange data exposure
- **Problem:** On `DateTimeParseException`, the method returns **all comments** instead of failing or returning an empty list. This can expose massive amounts of data and degrade performance.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/CommentService.java:527-546`
- **Fix:** Remove the catch-all fallback. Let the exception propagate, or catch it and return `List.of()` after logging the error. Never return the full dataset on invalid input.

#### 4.2 Fix ExerciseService.findByDateRange data exposure
- **Problem:** Same anti-pattern as 4.1: on parse failure it returns **all exercises**.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/ExerciseService.java:409-429`
- **Fix:** Same as 4.1 — return an empty list or propagate the exception. Do not return all exercises.

#### 4.3 Fix NPE risks in views
- **Problem:** Multiple views dereference potentially null values without guards.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/view/LessonsView.java:235` — `exercise.id.toString()` without null check.
  - `src/main/java/de/vptr/aimathtutor/view/UserSettingsView.java:81-83` — `getCurrentUser()` result used without null check.
  - `src/main/java/de/vptr/aimathtutor/view/UserSettingsView.java:267-268` — `getSettings()` result used without null check.
  - `src/main/java/de/vptr/aimathtutor/view/admin/AdminExercisesView.java:304-309` — `getCurrentUser()` result used without null check.
  - `src/main/java/de/vptr/aimathtutor/view/admin/AdminCommentsView.java:372` — session attribute may be null.
  - `src/main/java/de/vptr/aimathtutor/view/admin/AdminConfigView.java:426` — `getUserId()` may return null.
  - `src/main/java/de/vptr/aimathtutor/view/admin/AdminUserGroupsView.java:380` — `getAllUsers()` may return null.
  - `src/main/java/de/vptr/aimathtutor/component/layout/CommentsPanel.java:210` — `new Span(null)` throws NPE.
  - `src/main/java/de/vptr/aimathtutor/component/layout/CommentsPanel.java:399` — `event.getExerciseId().equals(this.exerciseId)` NPE if event ID is null.
- **Fix:** Add null checks before dereferencing. Use `Objects.requireNonNull()` with meaningful messages, or add early returns/guards.

#### 4.4 Fix missing null checks in AI service entry points
- **Problem:** `JsonRepairService`, `PromptBuilderService`, `MockAiProvider`, and `AiTutorService` do not validate null arguments, leading to `NullPointerException` instead of graceful degradation.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/service/ai/JsonRepairService.java:44`
  - `src/main/java/de/vptr/aimathtutor/service/ai/PromptBuilderService.java:98`
  - `src/main/java/de/vptr/aimathtutor/service/ai/provider/MockAiProvider.java:26`
  - `src/main/java/de/vptr/aimathtutor/service/AiTutorService.java:89,258`
- **Fix:** Add explicit null checks at the beginning of public methods and throw `IllegalArgumentException` with a descriptive message.

---

### Phase 5: Vaadin UI Threading & Lifecycle (High / Medium)

#### 5.1 Fix LoginView synchronous auth on UI thread
- **Problem:** `authService.authenticate()` is called directly inside the button click listener, blocking the UI thread.
- **Where:** `src/main/java/de/vptr/aimathtutor/view/LoginView.java:67`
- **Fix:** Offload to `CompletableFuture.supplyAsync(() -> this.authService.authenticate(username, password))`, then update UI inside `ui.access()`. Follow the exact project pattern: capture `ui = getUI().orElse(null)`, null-check, use `supplyAsync`, `thenAccept`, and `.exceptionally()`.

#### 5.2 Fix MathWorkspaceView synchronous generateProblem
- **Problem:** `aiTutorService.generateProblem()` is called synchronously during `buildUi()` and button clicks, blocking the UI while waiting for an external AI API.
- **Where:** `src/main/java/de/vptr/aimathtutor/view/MathWorkspaceView.java:204,489`
- **Fix:** Offload to async using the same `CompletableFuture` + `ui.access()` pattern. Show a loading indicator while waiting.

#### 5.3 Add onDetach cleanup to workspace views
- **Problem:** `ExerciseWorkspaceView` and `MathWorkspaceView` register JavaScript polling timers and `window.graspableViewConnector` but never clean them up on navigation. Async `CompletableFuture` chains are not cancelled.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/view/ExerciseWorkspaceView.java:356-411`
  - `src/main/java/de/vptr/aimathtutor/view/MathWorkspaceView.java:177-196`
- **Fix:** Override `onDetach()` in both views. Cancel any pending `CompletableFuture` instances, nullify `window.graspableViewConnector` via `executeJs`, and clear any pending JS timers.

#### 5.4 Fix UI.getCurrent() without null checks in async callbacks
- **Problem:** Async callbacks in `ExerciseWorkspaceView` and `MathWorkspaceView` capture `UI.getCurrent()` without checking for null. If called from a background thread where the UI is not set, this causes NPE.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/view/ExerciseWorkspaceView.java:479,541`
  - `src/main/java/de/vptr/aimathtutor/view/MathWorkspaceView.java:298,343`
- **Fix:** Use `getUI().orElse(null)` pattern with an explicit null check before calling `ui.access()`.

---

### Phase 6: Code Deduplication (Medium)

#### 6.1 Extract AI provider config loading helper
- **Problem:** `GeminiService`, `OpenAiService`, and `OllamaService` copy-paste identical dynamic config loading and clamping logic for `temperature` and `maxTokens`.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/service/GeminiService.java:73-79`
  - `src/main/java/de/vptr/aimathtutor/service/OpenAiService.java:94-99,201-206`
  - `src/main/java/de/vptr/aimathtutor/service/OllamaService.java:84-91`
- **Fix:** Add helper methods to `AiConfigService`: `getClampedTemperature(String key, double defaultValue)` and `getClampedTokens(String key, int defaultValue)`. Have all three services call these helpers.

#### 6.2 Remove redundant safeAnalyzeOllama / safeAnswerOllama wrappers
- **Problem:** `AiTutorService` has nearly identical `safeAnalyze`/`safeAnalyzeOllama` and `safeAnswer`/`safeAnswerOllama`. The Ollama-specific variants are redundant because `@Retry` on `OllamaAiProvider` already goes through the CDI proxy.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/AiTutorService.java:361-423`
- **Fix:** Remove the Ollama-specific wrappers. Use the generic `safeAnalyze` / `safeAnswer` methods for all providers.

#### 6.3 Extract admin CRUD base view
- **Problem:** Admin views (`AdminUsersView`, `AdminExercisesView`, `AdminCommentsView`, `AdminLessonsView`, etc.) duplicate constructor boilerplate, `buildUi()` patterns, dialog setup, grid action columns, and save-error handling.
- **Where:** All admin views under `src/main/java/de/vptr/aimathtutor/view/admin/`
- **Fix:** Create an abstract `AdminCrudView<T extends PanacheEntityBase, D>` base class with template methods for:
  - `getEntityClass()`, `getDtoClass()`
  - `buildGridColumns(Grid<D>)`
  - `createFormDialog(D dto)`
  - `saveDto(D dto)`
  - `deleteEntity(Long id)`
  Each concrete view extends the base and only implements the entity-specific parts.

#### 6.4 Deduplicate UserRankService boolean mapping
- **Problem:** `UserRankService.createRank`, `updateRank`, and `toDto` each contain ~18 lines of manual boolean field mapping, creating massive duplication.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/UserRankService.java:126-155,166-197,209-279`
- **Fix:** Use reflection or a static map of field names to extract/set the booleans in a loop. Alternatively, create a private `copyRankPermissions(UserRankEntity source, UserRankEntity target)` method and reuse it.

#### 6.5 Deduplicate GraspableMathService session completion
- **Problem:** `completeSession()` and `markSessionComplete()` implement the exact same logic.
- **Where:** `src/main/java/de/vptr/aimathtutor/service/GraspableMathService.java:123-132,253-265`
- **Fix:** Deprecate one and delegate to the other, or extract a private `doCompleteSession(String sessionId)` method.

---

### Phase 7: Accessibility (Low / Medium)

#### 7.1 Add aria-label to icon-only buttons
- **Problem:** All button components under `component/button/` set icons and tooltips but do not set `aria-label`, making them inaccessible to screen readers.
- **Where:** Every file under `src/main/java/de/vptr/aimathtutor/component/button/`
- **Fix:** In each button constructor, after setting the tooltip, also call `getElement().setAttribute("aria-label", tooltipText)`.

#### 7.2 Add text alternatives to color-only status indicators
- **Problem:** Status indicators in admin grids rely solely on CSS color with no textual alternative for screen readers.
- **Where:**
  - `src/main/java/de/vptr/aimathtutor/view/admin/AdminCommentsView.java:258-264`
  - `src/main/java/de/vptr/aimathtutor/view/admin/AdminSessionsView.java:133`
  - `src/main/java/de/vptr/aimathtutor/view/admin/AdminUserRanksView.java:170-373`
- **Fix:** Add `aria-label` or visible text prefixes (e.g., "Active: ", "Banned: ") to status components so color is not the only channel conveying information.

---

### Phase 8: Tests & CI/CD (High / Medium)

#### 8.1 Delete trivial getter/setter tests
- **Problem:** ~30+ test files consist only of trivial getter/setter tests that assign a value and assert it was stored. They provide almost no behavioral coverage and create maintenance noise.
- **Where:**
  - All files under `src/test/java/de/vptr/aimathtutor/dto/`
  - All files under `src/test/java/de/vptr/aimathtutor/entity/`
  - `src/test/java/de/vptr/aimathtutor/event/CommentCreatedEventTest.java`
- **Fix:** Delete these files entirely. Replace with meaningful behavioral tests where appropriate.

#### 8.2 Add security-focused tests
- **Problem:** There are zero tests for critical security classes.
- **Where:** Missing tests for:
  - `LoginAttemptService`
  - `RateLimitService`
  - `UserIdentityProvider`
  - `UserRankIdentityAugmentor`
  - `CommentPermissionService`
  - `CommentFlaggingService`
  - `CommentModerationService`
  - `CommentRateLimitService`
  - `AuthService` (only null/empty input is tested)
- **Fix:** Add tests covering:
  - Valid login with correct credentials
  - Wrong password rejection
  - Banned/inactive user rejection
  - Login throttling / lockout behavior
  - Role augmentation for different ranks
  - Comment permission matrix (author vs moderator vs stranger)

#### 8.3 Fix CI to run integration tests
- **Problem:** `pom.xml` sets `<skipITs>true</skipITs>` and CI runs `./mvnw test`, so repository integration tests are never executed.
- **Where:** `.github/workflows/ci-cd.yml` and `pom.xml:25`
- **Fix:** Either set `skipITs=false` in CI or change the CI command to `./mvnw verify`.

#### 8.4 Fix test DB leakage
- **Problem:** Repository ITs annotate test methods with `@Transactional`, which commits at the end of the test in Quarkus, leaving persisted data in the shared test database.
- **Where:** `CommentRepositoryIT.java`, `ExerciseRepositoryIT.java`, `StudentSessionRepositoryIT.java`
- **Fix:** Replace `@Transactional` with `@io.quarkus.test.TestTransaction` so tests roll back automatically.

#### 8.5 Update GitHub Actions cache version
- **Problem:** CI uses `actions/cache@v3` which is outdated.
- **Where:** `.github/workflows/ci-cd.yml:67`
- **Fix:** Update to `actions/cache@v4`.

---
