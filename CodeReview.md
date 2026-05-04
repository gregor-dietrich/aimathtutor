# Full Code Review & Remediation Plan

## Recommended Implementation Order

1. **Phase 1:** Critical/Security Issues — address these first, one by one
2. **Phase 2:** High-Priority Bugs & Architectural Issues — start with UI thread safety (H2) and auth annotations (H3)
3. **Phase 3:** Moderate Code Quality & Duplication Issues, Pt. 1 — tackle M1–M6 (extraction/refactoring) as a dedicated refactoring sprint
4. **Phase 4:** Moderate Code Quality & Duplication Issues, Pt. 2 — tackle M7-M20
5. **Phase 5:** Test Coverage & CI — focus on AI service mocks first
6. **Phase 6:** Low-Priority / Cosmetic — batch low-priority items

---

## Phase 1: Critical/Security Issues (Immediate)

| #   | Issue                                                                                        | Location                                                               | Action                                                              |
| --- | -------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------- |
| S1  | **Invalid bcrypt dummy hash** breaks timing-attack mitigation                                | `UserIdentityProvider.java:77`                                         | Replace with valid 60-char bcrypt hash                              |
| S2  | **No session fixation protection** — session ID not regenerated on login                     | `AuthService.java:109-113`                                             | Invalidate old HTTP session, create new one after auth              |
| S3  | **Comment XSS sanitizer** uses naive regex `<[^>]*>`                                         | `CommentService.java:297-302`                                          | Replace with OWASP HTML Sanitizer or rely on Vaadin auto-escaping   |
| S4  | **User impersonation in `createComment`** — caller can set arbitrary `comment.user`          | `CommentService.java:164-201`                                          | Always derive user from `currentUsername`, never trust entity input |
| S5  | **Missing error notifications** in async AI feedback handlers — user sees nothing on failure | `MathWorkspaceView.java:396-401`, `ExerciseWorkspaceView.java:553-558` | Add `Notification.show()` in `exceptionally` handlers               |

## Phase 2: High-Priority Bugs & Architectural Issues

| #   | Issue                                                                                                | Location                                                                                                   | Action                                                                      |
| --- | ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| H1  | **N+1 query in `LessonService.isDescendantOf`** — lazy-loads parent chain one-by-one                 | `LessonService.java:203-211`                                                                               | Write recursive CTE query or use bounded eager fetch                        |
| H2  | **`AsyncDataLoader` calls `component.getUI()` off UI thread**                                        | `AsyncDataLoader.java:65`                                                                                  | Capture `UI` reference before async call, use `ui.access()`                 |
| H3  | **No declarative auth annotations** (`@RolesAllowed`, `@Authenticated`) on any route                 | All 16 route views                                                                                         | Add `@Authenticated` to user views, `@RolesAllowed("admin")` to admin views |
| H5  | **No IP-based rate limiting** — only per-username                                                    | `LoginAttemptService.java`                                                                                 | Add IP-based throttling alongside username tracking                         |
| H6  | **AI provider config cache has no expiry + race condition**                                          | `AiConfigService.java:38,102-121`                                                                          | Use Caffeine cache with TTL, or `ConcurrentHashMap.computeIfAbsent`         |
| H8  | **LazyInitializationException risk** in ViewDto constructors accessing `.size()` on lazy collections | `UserViewDto.java:46-47`, `ExerciseViewDto.java:57`, `UserRankViewDto.java:94`, `UserGroupViewDto.java:23` | Use eager JPQL fetch or `@Transactional`                                    |

## Phases 3 & 4: Moderate Code Quality & Duplication Issues

| #   | Issue                                                                                                                          | Location                                                                                                         | Action                                                                                                          |
| --- | ------------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| M1  | **3x duplicated AI provider logic** (config loading, API key validation, empty-response check, `isConfigured()`, `getModel()`) | OpenAiService, GeminiService, OllamaService                                                                      | Extract into abstract base class or utility                                                                     |
| M2  | **2x duplicated `generateContent`/`generateJsonContent`** within OpenAiService (~80% identical)                                | `OpenAiService.java:84-254`                                                                                      | Extract shared `doGenerate(prompt, systemPrompt, jsonMode)`                                                     |
| M3  | **3x duplicated save pattern** in AdminConfigView                                                                              | `AdminConfigView.java:483-543`                                                                                   | Extract `saveProviderConfig(List<AiConfigUpdateDto>)` helper                                                    |
| M4  | **3x duplicated provider panel builder** in AdminConfigView (~480 lines)                                                       | `AdminConfigView.java:181-348`                                                                                   | Parameterize panel builder with config prefix + field defaults                                                  |
| M5  | **7x duplicated permission column pattern** in AdminUserRanksView (248-line method)                                            | `AdminUserRanksView.java:145-393`                                                                                | Extract `createPermissionColumn(String name, Function<BooleanGetter>)`                                          |
| M6  | **Duplicated topbar code** between MainLayout and AdminMainLayout                                                              | `MainLayout.java:191-252`, `AdminMainLayout.java:253-295`                                                        | Extract shared `TopBarComponent` or utility                                                                     |
| M7  | **Error handling inconsistency across AI services** — different exception types, missing safety checks                         | All three AI services                                                                                            | Standardize: custom `AiProviderException` with HTTP status, add truncation/completeness checks to Gemini/Ollama |
| M9  | **Sync service calls blocking UI thread** across multiple views                                                                | LessonsView, UserSettingsView, ExerciseWorkspaceView, AdminExercisesView, AdminSessionsView, AdminUserGroupsView | Wrap in `AsyncDataLoader` or `CompletableFuture.supplyAsync`                                                    |
| M10 | **DB auth query on every navigation** — `isAuthenticated()` hits DB every `beforeEnter`                                        | `AuthService.java:154-174`                                                                                       | Cache auth result with short TTL or use session-based check                                                     |
| M11 | **TOCTOU race in `CommentFlagRepository.createFlag`**                                                                          | `CommentFlagRepository.java:60-88`                                                                               | Use DB unique constraint + catch `ConstraintViolationException` for clean response                              |
| M13 | **Config key magic strings** throughout AdminConfigView                                                                        | `AdminConfigView.java`                                                                                           | Extract to `AiConfigKeys` constants class                                                                       |
| M15 | **`DifficultyLevel.fromString()` returns null** on unrecognized value                                                          | `DifficultyLevel.java:38-48`                                                                                     | Return `Optional` or throw `IllegalArgumentException`                                                           |
| M17 | **Inconsistent `initialized` flag pattern** across views                                                                       | Multiple                                                                                                         | Standardize: always use `initialized` flag for views that rebuild on navigation                                 |

## Phase 5: Test Coverage & CI

| #   | Issue                                                                                                                                                    | Action                                                                              |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| T1  | **No tests at all** for OpenAiService, GeminiService, OllamaService, GraspableMathService, ProblemGeneratorService                                       | Add unit tests with mocked HTTP clients                                             |
| T2  | **Service tests only test validation** — UserService, UserGroupService, LessonService, ExerciseService, CommentService tests only cover null/empty input | Add happy-path and integration tests                                                |
| T3  | **Duplicate test**: `PasswordUtilityTest` tests same class as `PasswordHashingServiceTest`                                                               | Merge into one test class                                                           |
| T4  | **No `@Retry` behavior tests** for AI services                                                                                                           | Add tests verifying retry on transient failure and abort on `IllegalStateException` |
| T5  | **`ThemeServiceTest` creates `new ThemeService()`** — will break if CDI dependencies added                                                               | Use `@Inject` via `@QuarkusTest`                                                    |
| T6  | **CI pipeline missing OWASP dep-check and secret scanning** (commented out)                                                                              | Uncomment and configure `NVD_API_KEY` repository secret                             |
| T7  | **CI security job lacks Maven cache**                                                                                                                    | Add `actions/cache@v4` to security job                                              |
| T8  | **No `.env.example` template** in repository                                                                                                             | Create one documenting all required env vars                                        |

## Phase 6: Low-Priority / Cosmetic

| #   | Issue                                                              | Action                                                                                                                                                                              |
| --- | ------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| L1  | `LessonViewDto` catches `LazyInitializationException` — code smell | Use eager JPQL fetch instead                                                                                                                                                        |
| L4  | No session timeout configured                                      | ~~Add `quarkus.http.session-timeout=30M`~~ — **NOTE:** suggested config property does not exist in Quarkus 3.33, need to find another way to handle this, or can we leave it as is? |

---
