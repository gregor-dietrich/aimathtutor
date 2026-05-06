# Code Quality, Maintainability & Security Standards — AIMathTutor

Expected of all contributors. Before submitting, run quality gates. Reviewers verify before approving.

## Principles

- Clarity over cleverness. Self-documenting code. Single Responsibility.
- Eliminate duplication immediately. Extract shared logic. No copy-paste.
- Security by default: validate all input, least-privilege, no hardcoded secrets.
- Performance: avoid N+1 queries, use pagination/caching, profile before optimizing.

## Quality Gates

**Never skip checkstyle or SpotBugs.** Checkstyle severity is `error` — violations block build.

```shell
make test                              # run tests
./mvnw checkstyle:check                # style check (Google Java Style)
./mvnw spotbugs:check                  # static analysis
./mvnw org.owasp:dependency-check-maven:check  # CVE scan (needs NVD_API_KEY)
./mvnw license:add-third-party         # license report (verify phase)
```

CI order: `test` → `security` (CodeQL) → `build` (package + SpotBugs + Checkstyle).

## Code Smell Checklist

| Smell                | Threshold                            | Fix                               |
| -------------------- | ------------------------------------ | --------------------------------- |
| Large class          | >300 lines                           | Split into focused classes        |
| Long method          | >30 lines                            | Extract helper methods            |
| Duplicate code       | Same logic in 2+ places              | Extract to shared method/class    |
| Long parameter list  | >5 params                            | Use DTOs                          |
| Magic numbers        | Unexplained constants                | Named constant or enum            |
| Unclear naming       | `x`, `temp`, `data`                  | Descriptive names matching intent |
| Deep nesting         | 3+ levels of if/for                  | Extract methods or early returns  |
| Dead code            | Unused variables/methods             | Remove                            |
| Catch-all exceptions | `catch (Exception e)`                | Catch specific exceptions         |
| Silent failures      | Swallowed exceptions without logging | Always log or rethrow             |
| God object           | One class doing everything           | Refactor into smaller classes     |
| Hardcoded values     | Config baked into code               | Use env vars or DB-backed config  |
| No tests             | New logic without coverage           | Add tests                         |

## Enforced Checkstyle Rules (Beyond Google Style)

| Rule                  | Enforcement                       | Fix                                            |
| --------------------- | --------------------------------- | ---------------------------------------------- |
| No FQCNs              | `RegexpSinglelineJava`            | Always use imports                             |
| No SLF4J              | `IllegalImport`                   | Use `org.jboss.logging.Logger`                 |
| No `*v` log methods   | `RegexpSinglelineJava`            | Use `*f` methods (`infof`, `debugf`) with `%s` |
| No direct UlidCreator | `IllegalImport`                   | Use `UlidUtil` wrapper                         |
| 4-space indent        | `FileTabCharacter` + indent rules | No tabs, 4 spaces                              |

## Security Checklist

- [ ] No hardcoded secrets (passwords, tokens, API keys)
- [ ] Input validation on all user-provided data
- [ ] No SQL injection (parameterized queries only)
- [ ] Session-based auth via `VaadinSession` + `PermissionService`
- [ ] Sensitive data not logged
- [ ] Error messages don't leak internals (paths, stack traces, SQL)
- [ ] Dependencies checked for CVEs
- [ ] AI API keys from env vars only, never logged
- [ ] All `@Inject` fields in Vaadin views are `transient`

## Performance Checklist

- [ ] No N+1 queries (joins/batch fetches, not loops)
- [ ] `@Transactional` only on mutating service methods
- [ ] Lazy-loaded relationships (`@ManyToOne(fetch = LAZY)`)
- [ ] Async UI: `CompletableFuture.supplyAsync()` + `ui.access()` in views
- [ ] AI provider calls use `@Retry` + `@Timeout`
- [ ] Connection pooling configured (Quarkus default)

## References

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Quarkus Best Practices](https://quarkus.io/guides/)
- [Vaadin Flow Documentation](https://vaadin.com/docs/latest/)
