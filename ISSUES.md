# Open Issues

Issues surfaced during the 2026-05-20 code review. The security findings S1–S4 and the performance findings P1–P8 have already been landed; everything below is queued for follow-up.

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

### C5. [INFO] `User.findAllOrdered` named query lacks a supporting index on `created`

**File:** `src/main/java/de/vptr/aimathtutor/entity/UserEntity.java:35`

The query now eagerly fetches `u.rank`, but still sorts on `u.created` without a dedicated index. On large user tables this falls back to a sequential scan + sort.

**Fix:** Add `@Index(name = "idx_user_created", columnList = "created DESC")` to `UserEntity`'s `@Table` and a matching `CREATE INDEX users_created_idx ON users (created DESC);` in `init.sql`.
