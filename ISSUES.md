# Open Issues

Issues surfaced during the 2026-05-20 code review. The security findings S1–S4 and the performance findings P1–P8 have already been landed; everything below is queued for follow-up.

Each entry lists severity, file:line, what's wrong, and a suggested fix. Sorted by severity within each section.

---

## Security

### S8. [INFO] Encryption KDF uses single HMAC, not full HKDF

**File:** `src/main/java/de/vptr/aimathtutor/service/security/EncryptionService.java:128-136`

`deriveKey(master, label)` is `HMAC(master, label)` rather than HKDF-Extract+Expand. For two well-separated labels (`"encrypt"`, `"blind-index"`) this is acceptable and indistinguishable in practice. Flagging for awareness — if a third sub-key is ever added, switch to HKDF.

---

## Correctness / Code Quality

### C4. [LOW] Missing tests for security-critical edge cases

- Concurrent `EncryptionKeyManager.generateAndPersistKey()` — the `FileAlreadyExistsException` fallback at line ~121 is uncovered.
- SSRF allowlist bypass attempts (mixed-case host, IPv6, URL-encoded host, host with trailing dot).
- `LoginAttemptService` clock skew at the 3600 s cap boundary.

**Fix:** Add unit tests under `src/test/java/de/vptr/aimathtutor/service/security/`. The `LoginAttemptServiceTest` cap-at-3600 assertion is already strict per AGENTS.md — keep that pattern and extend it.
