# Open Issues

Issues surfaced during the 2026-05-20 code review. The four highest-priority security findings (S1–S4) have already been landed; everything below is queued for follow-up.

Each entry lists severity, file:line, what's wrong, and a suggested fix. Sorted by severity within each section.

---

## Security

### S5. [LOW] Auth-cache TTL means revocation takes up to 30 s to take effect

**File:** `src/main/java/de/vptr/aimathtutor/service/security/AuthService.java:76, 270-286`

`AUTH_CACHE_TTL_MILLIS = 30_000L` skips the DB check during route navigations. Banning or deactivating a user (admin action) does not invalidate existing sessions for up to 30 s. This is a documented, deliberate tradeoff but worth flagging.

**Fix (optional):** On admin ban/deactivate, evict the affected session's cache marker; or reduce the TTL to ~5 s. Acceptable to leave as-is given the latency win.

### S6. [LOW] No account-wide rate limit (distributed credential stuffing)

**Files:** `src/main/java/de/vptr/aimathtutor/service/security/LoginAttemptService.java`, `src/main/java/de/vptr/aimathtutor/service/security/AuthService.java:101-113`

Throttling is keyed by `username:ip` and `ip`. A botnet rotating across thousands of IPs can keep hitting one username at slow per-IP rates without ever triggering a lockout. The 1 h cap (`MAX_LOCKOUT_SECONDS = 3600`) is correct and tested — **do not change that**.

**Fix:** Add a third bucket keyed by `username` alone with a higher threshold (e.g. 25 failures / 15 min triggers a 10 min soft-lock). Keep the per-`username:ip` bucket so a single attacker can't lock a real user out forever from elsewhere.

### S7. [INFO] Default DB password `changeit` and seeded `admin/admin` account

**Files:** `src/main/resources/application.properties:11`, `src/main/resources/sql/init.sql`

Production schema strategy is `validate` and `init.sql` only loads under `%dev,test`, so seeded accounts won't make it into a production schema as-is. But `quarkus.datasource.password=changeit` is the baked-in default.

**Fix:** Document the env-var override (`QUARKUS_DATASOURCE_PASSWORD`) in the README's deployment section; optionally fail fast in `AppLifecycleBean` if the production profile is active and the password is still `changeit`.

### S8. [INFO] Encryption KDF uses single HMAC, not full HKDF

**File:** `src/main/java/de/vptr/aimathtutor/service/security/EncryptionService.java:128-136`

`deriveKey(master, label)` is `HMAC(master, label)` rather than HKDF-Extract+Expand. For two well-separated labels (`"encrypt"`, `"blind-index"`) this is acceptable and indistinguishable in practice. Flagging for awareness — if a third sub-key is ever added, switch to HKDF.

---

## Performance

### P1. [HIGH] N+1 lazy loads when building `ExerciseViewDto` from `ExerciseEntity`

**Files:** `src/main/java/de/vptr/aimathtutor/dto/ExerciseViewDto.java:64-87`, `src/main/java/de/vptr/aimathtutor/entity/ExerciseEntity.java:36, 67, 72, 92`, `src/main/java/de/vptr/aimathtutor/repository/ExerciseRepository.java:25-39`

`ExerciseEntity.user` (`@ManyToOne(LAZY)`), `.lesson` (`@ManyToOne(LAZY)`), and `.comments` (`@OneToMany`) are all lazy. The named query `Exercise.findAllOrdered` has no `JOIN FETCH`. The DTO constructor reads `entity.user.publicId`, `entity.user.username`, `entity.lesson.publicId`, `entity.lesson.name`, **and** `entity.comments.size()` for every row. `comments.size()` forces the collection to load.

`exerciseService.getAllExercises()` returns up to 500 rows in `AdminExercisesView.loadExercises()`. Worst case: 1 + 500 × 3 ≈ 1500 SELECTs per admin grid load.

**Fix:** Add an `Exercise.findAllOrderedWithRelations` named query with `LEFT JOIN FETCH e.user LEFT JOIN FETCH e.lesson` plus a projected `commentsCount` (or `@Formula`). Apply same pattern to `findPublished()`, `findByUserId()`, `findByLessonId()` where the DTO is the consumer.

### P2. [HIGH] `UserRepository.findAll()` returns whole table; `UserService.getAllUsers()` has no pagination on the default path

**Files:** `src/main/java/de/vptr/aimathtutor/repository/UserRepository.java:140-141, 213-217`, `src/main/java/de/vptr/aimathtutor/service/UserService.java`

Default `findAll()` is unbounded. Combined with `UserEntity.rank` being `@ManyToOne(LAZY)`, the admin users grid lazy-loads the rank for each row when the column renderer reads it.

**Fix:** Require an explicit page/pageSize on `getAllUsers()`. Default the admin grid to a `CallbackDataProvider` so the DB is queried per page. Add `JOIN FETCH u.rank` to the paginated named query if the grid renders rank.

### P3. [HIGH] `CommentRepository.findAllOrderedWithRelations()` default is `Integer.MAX_VALUE`

**File:** `src/main/java/de/vptr/aimathtutor/repository/CommentRepository.java:33`

Convenience overload calls the paginated version with `Integer.MAX_VALUE`, returning every row × 3 LEFT JOINs (`c.user`, `c.exercise`, `c.parentComment`). On a production-sized table this is a memory/IO landmine.

**Fix:** Remove the no-arg overload or cap at a sane page size (e.g. 200). Force callers to pass an explicit pagination window. Update callers in `AdminCommentsView` and `CommentService.getAllComments()`.

### P4. [HIGH] AdminDashboardView serial fan-out of analytics queries

**Files:** `src/main/java/de/vptr/aimathtutor/view/admin/AdminDashboardView.java:~150-180`, `src/main/java/de/vptr/aimathtutor/service/AnalyticsService.java`

The dashboard runs ~10+ analytics calls per load. Several (`getAllUsersProgressSummary`, `getRecentSessions(10)`, `getTopStudentsByCompletion`) load the full `StudentSessionEntity` table via `findAll()` then filter/sort/group in Java. With `StudentSession.find*WithRelations` queries using `LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise`, every full scan pulls 2× wide rows.

**Fix:** Replace in-memory aggregates with SQL: `COUNT()`/`GROUP BY`/`ORDER BY ... LIMIT` projection queries that return DTOs directly (`SELECT NEW de.vptr...Dto(...)`). For "recent N", use a `LIMIT` query. Drop `JOIN FETCH` on full-scan analytics queries — those don't need eager-loaded entities, just projected fields.

### P5. [MEDIUM] Pagination + `LEFT JOIN FETCH` collections risk Cartesian explosions

**File:** `src/main/java/de/vptr/aimathtutor/entity/CommentEntity.java:30-74`

Existing `LEFT JOIN FETCH` targets in Comment queries are all to-one (`c.user`, `c.exercise`, `c.parentComment`), so this is fine **today**. If anyone adds a `JOIN FETCH` on a `@OneToMany` to a query that also paginates, Hibernate can't push `LIMIT` to the DB and emits `HHH000104`, applying the limit in memory.

**Fix:** Add a project rule (Checkstyle regex or PR-review checklist): never `JOIN FETCH` a collection in a query that also paginates.

### P6. [MEDIUM] No caching for read-mostly reference data

**Files:** `src/main/java/de/vptr/aimathtutor/service/UserRankService.java`, `src/main/java/de/vptr/aimathtutor/service/ai/AiConfigService.java`

`AiConfigService` already has its own in-memory `configCache` (invalidated on update — good). Ranks are not cached and are queried per request.

**Fix:** Cache `getAllRanks()` with `@CacheResult(cacheName = "ranks")` and invalidate via `@CacheInvalidateAll` from any rank-modification endpoint. Quarkus's built-in cache is sufficient.

### P7. [MEDIUM] `AsyncDataLoader` has no per-call timeout override

**File:** `src/main/java/de/vptr/aimathtutor/util/AsyncDataLoader.java:~70`

All async loads use `AppConstants.ADMIN_ASYNC_TIMEOUT_SECONDS`. The dashboard fans out 10+ tasks; a single slow analytics query can hold a whole panel until the global timeout fires.

**Fix:** Add an overload accepting a `Duration timeout` so dashboard panels can fail fast (5 s) and heavy reports can wait longer (60 s).

### P8. [INFO] Comments collection size triggers full load

**File:** `src/main/java/de/vptr/aimathtutor/dto/ExerciseViewDto.java:78`

`entity.comments.size()` on a Panache `@OneToMany` collection issues a `SELECT *` for the collection rather than `SELECT COUNT(*)`. For grid cells this is wasted IO. Sub-issue of P1.

**Fix:** Project `commentsCount` directly in the SQL/JPQL when building the DTO, or use `@Formula("(SELECT COUNT(*) FROM comments WHERE comments.exercise_id = id)")` on the entity.

---

## Correctness / Code Quality

### C1. [MEDIUM] Generic exception handling masks moderator-id-null in `AdminCommentsView`

**Files:** `src/main/java/de/vptr/aimathtutor/view/admin/AdminCommentsView.java:477, 497, 517`, `src/main/java/de/vptr/aimathtutor/service/comment/CommentModerationService.java:63-66`

The view passes `authService.getUserId()` (`@Nullable`) to `moderateComment()` without checking. The service correctly rejects null with `WebApplicationException("Moderator ID is required", BAD_REQUEST)`, but the view's catch-all `catch (final Exception e)` swallows it as a generic "An error occurred while hiding the comment." Real cause hidden from logs (the original message is dropped).

**Fix:** Add an explicit `if (currentUserId == null) { NotificationUtil.showError("Session expired, please log in again"); return; }` before each call site. Same pattern at lines 354 and 378.

### C2. [MEDIUM] `EncryptionService.decrypt()` includes raw header in exception message

**File:** `src/main/java/de/vptr/aimathtutor/service/security/EncryptionService.java:95`

`throw new IllegalArgumentException("Unrecognized ciphertext envelope version: " + parts[0])` — `parts[0]` is attacker-influenceable if someone writes into an encrypted column via a non-converter path (unlikely, but defence-in-depth). Logging the version is fine; throwing the raw value in an exception that may surface to error pages is not.

**Fix:** `throw new IllegalArgumentException("Unrecognized ciphertext envelope")` and log `parts[0]` at `DEBUG`.

### C3. [LOW] AI response parse fallback hides errors

**File:** `src/main/java/de/vptr/aimathtutor/service/ai/JsonRepairService.java:~76-83`

On any `IOException` from Jackson, the code falls back to regex extraction and returns a hardcoded `AiFeedbackDto.hint()`. Genuine provider errors (rate-limit JSON, error envelopes) get silently coerced into a hint — monitoring and bug reports become useless.

**Fix:** Distinguish truncation from malformed JSON: if the JSON ends with an opening brace or is short, treat as truncation and call the repair path; otherwise log at `WARN` with the raw body and return an explicit `AiFeedbackDto.error(...)`.

### C4. [LOW] Missing tests for security-critical edge cases

- Concurrent `EncryptionKeyManager.generateAndPersistKey()` — the `FileAlreadyExistsException` fallback at line ~121 is uncovered.
- SSRF allowlist bypass attempts (mixed-case host, IPv6, URL-encoded host, host with trailing dot).
- `LoginAttemptService` clock skew at the 3600 s cap boundary.

**Fix:** Add unit tests under `src/test/java/de/vptr/aimathtutor/service/security/`. The `LoginAttemptServiceTest` cap-at-3600 assertion is already strict per AGENTS.md — keep that pattern and extend it.

### C5. [INFO] `UserEntity.findAllOrdered` named query returns whole table without index hint

**File:** `src/main/java/de/vptr/aimathtutor/entity/UserEntity.java:35`

`@NamedQuery(name = "User.findAllOrdered", query = "FROM UserEntity ORDER BY created DESC")` — confirm `created` has a btree index in `init.sql`. If not, full table scan + sort.

**Fix:** Verify (and add if missing) `CREATE INDEX users_created_idx ON users (created DESC);` in `init.sql`.

### C6. [INFO] AppConfig uses class-level `@Push` (correct per AGENTS.md)

**File:** `src/main/java/de/vptr/aimathtutor/AppConfig.java`

This is fine; noted because the dashboard analytics findings (P4) imply some long-running queries, and combined with `@Push`, an unbounded analytics load could keep the WebSocket open while it spins. The Vaadin-thread fix in P4 covers it.
