---
applyTo: "**"
---

# AIMathTutor — Agent Guide

## Build & Development

- **Primary interface:** `make` commands, not raw Maven. See `Makefile` or run `make help`.
- **Java 25 is required.** `make check` enforces this; CI uses Temurin JDK 25.
- **Maven wrapper:** `./mvnw` (scripts fall back to system `mvn` if available).
- **Dev mode:** `make dev` → `./mvnw quarkus:dev` on port `9001`. Dev UI: `http://localhost:9001/q/dev/`.
- **Tests:** `make test` → `./mvnw test`. Uses `@QuarkusTest`, Mockito, Panache Mock.
- **Install (skip tests):** `make install` → `./mvnw clean install -DskipTests`.
- **Production build:** Must pass `-Pproduction` to trigger Vaadin `prepare-frontend` + `build-frontend`. CI build command: `./mvnw clean install package -DskipTests -Pproduction`.
- **JVM args required:** `--add-opens java.base/java.lang=ALL-UNNAMED` and `-XX:+EnableDynamicAgentLoading` (configured in `quarkus-maven-plugin`).
- **Versioning:** Controlled by Maven property `${revision}` (default `1.0.0-SNAPSHOT`). Pass `-Drevision=X.Y.Z` when needed.

## Architecture

- **Monolithic Quarkus + Vaadin.** No REST boundary between frontend views and backend services.
- **Vaadin views inject services via CDI (`@Inject`).** REST clients are **only** for external AI APIs.
- **Main packages:** `entity/` (Panache), `repository/` (Panache), `service/` (`@ApplicationScoped`), `view/` (Vaadin), `dto/`, `security/`.

## Coding Conventions

- **Indentation:** All `*.java` files must use **4 spaces** for indentation. No tabs allowed.
- **Imports:** Never use fully qualified class names in code. Always add an `import` statement and use the short class name. This applies to all Vaadin components (e.g., `HorizontalLayout`, `FlexComponent`, `Checkbox`, `ComboBox`, `Text`) and any other external classes.
- **Vaadin UI threading:** Never block the UI thread with long-running or network operations. Use `CompletableFuture.supplyAsync()` to run blocking calls off the UI thread, then update UI inside `ui.access()`. Always handle exceptions with `.exceptionally()` to show error notifications. Pattern:

```java
final var ui = getUI().orElse(null);
if (ui == null) {
    return;
}
CompletableFuture.supplyAsync(blockingCall::get).thenAccept(result -> {
    ui.access(() -> {
        // update UI here
    });
}).exceptionally(ex -> {
    ui.access(() -> {
        // show error notification
    });
    return null;
});
```

- **@Push:** Server push is enabled globally via `@Push` on `AppConfig`. Individual views do not need their own `@Push` annotation.

### Critical Anti-Patterns (Do Not Propose)

- **Do NOT make LoginView async.** Wrapping `authService.authenticate()` in `CompletableFuture.supplyAsync()` causes `ContextNotActiveException` on navigation because `ui.access()` has no CDI request context. `MainLayout.beforeEnter()` calls `isAuthenticated()` which needs the EntityManager. Keep login synchronous. Tried and reverted multiple times.
- **Use `QuarkusSecurityIdentity.builder(identity)` when augmenting** — it preserves credentials, attributes, and permission checkers. Using `builder()` loses original roles. EXCEPTION: When roles need normalization (e.g. `UserRankIdentityAugmentor`), use `builder()` and manually copy credentials/attributes, since `builder(identity)` copies original roles un-normalized.
- **CommentsPanel must NOT have `@Observes` methods.** It is instantiated with `new`, not CDI. Real-time refresh uses `CommentCreatedEventBridge` with programmatic listeners.
- **ConversationContextDto fields must stay `private final` with unmodifiable getters.** Do not revert to public fields.
- **`VaadinSession.getCurrent()` can be null.** Always null-check before use. This applies to `AuthService.getUsername()`, `logout()`, `isAuthenticated()`.
- **MathWorkspaceView request ID staleness checks must stay.** The `problemRequestId` counter, `pendingProblemFuture.cancel()` calls, and JS-side `window.currentProblemRequestId` check prevent race conditions on rapid problem generation.
- **All `@Inject` fields in Vaadin views must be `transient`.** Vaadin serializes views for UI state.
- **In `onDetach(DetachEvent)`, use `detachEvent.getUI()` not `getUI()`.** The latter may return empty during detach.
- **LoginAttemptServiceTest must verify exact cap value of 3600.** Do not revert to weak `<= 3600` assertion.
- **RateLimitServiceTest must use `UUID.randomUUID()` for user IDs.** Hardcoded strings cause state leakage between tests since the service is `@ApplicationScoped`.
- **AdminConfigView save methods must null-check `authService.getUserId()` before use.** The `requireUserId()` helper enforces this; do not bypass it.
- **Do NOT use FQCNs.** Always use proper imports instead of fully qualified class names.

## Code Quality Gates

All are enforced in CI (`build` job); run locally before pushing:

| Gate            | Command                                         | Notes                                               |
| --------------- | ----------------------------------------------- | --------------------------------------------------- |
| Tests           | `make test`                                     |                                                     |
| SpotBugs        | `./mvnw spotbugs:check`                         | Fails build; exclusions in `spotbugs-exclude.xml`   |
| Checkstyle      | `./mvnw checkstyle:check`                       | Google Java Style; config in `checkstyle.xml`       |
| OWASP dep-check | `./mvnw org.owasp:dependency-check-maven:check` | Requires `NVD_API_KEY` env var; `failBuildOnCVSS=7` |
| License report  | `./mvnw license:add-third-party`                | Runs at `verify` phase                              |

- **CI order:** `test` → `security` (CodeQL) → `build` (package + SpotBugs + Checkstyle).

## Database

- **PostgreSQL.** Dev/test uses Quarkus devservices (`postgres:18.3-alpine3.23` image on port `55432`).
- **Schema strategy:**
  - **Dev/Test:** `drop-and-create`, loads `src/main/resources/sql/init.sql`.
  - **Production:** `validate`, expects schema already present; production seed is `sql/init.sql`.
- **Test accounts (dev/test seed):** `admin`/`admin`, `teacher`/`teacher`, `student1`/`student1`, `student2`/`student2`.
- **Password utility:** `make password` generates salt+hash for `init.sql` inserts.

## AI Configuration

- **API keys (env vars, immutable):** `GEMINI_API_KEY`, `OPENAI_API_KEY`, `OPENAI_ORG_ID`.
- **Runtime settings (DB-backed, mutable):** Model, temperature, max tokens, prompts, etc. configured via **Admin Settings UI** at `/admin/config`.
- **Mock provider:** Set `ai.tutor.provider=mock` or `ai.tutor.enabled=false` for testing without external APIs.
- **Test profile overrides:** Disables `@Retry` delays on `AiTutorService` Ollama calls and sets 1-second timeouts to fail fast.

## Changelog

- Maintain per-version files in `changelog/` (e.g., `changelog/2.2.5.md`).
- Keep an `Unreleased.md` section; move items to a new version file at release time.
- Follow [Keep a Changelog](https://keepachangelog.com) format. Do **not** mention class/method names—describe user-facing changes only.

## Docker

- **Production:** `docker-compose.yml` in repo root (app + PostgreSQL; optional pgadmin/Ollama).
- **Dockerfiles:** `src/main/docker/Dockerfile.alpine` and `Dockerfile.ubuntu` (port 9001, healthcheck on `/q/health/ready`).
- **Build script:** `scripts/sh/build.sh` (used by `make build`) handles multi-platform `docker buildx` with QEMU fallback.

## Key Files

- `pom.xml` — Maven config, versions, quality plugins, profiles (`native`, `production`).
- `src/main/resources/application.properties` — Quarkus config, AI env var wiring, Hibernate strategy.
- `Makefile` / `scripts/sh/` — Dev commands, git helpers (`branch`, `rebase`, `tag`, `release`).
- `.github/workflows/ci-cd.yml` — GitHub Actions pipeline.
- `docs/QUICKSTART.md` — Setup, Docker Compose, Ollama model recommendations.
- `docs/BUILD_GUIDE.md` — JDK/Maven requirements, packaging, Docker images.
