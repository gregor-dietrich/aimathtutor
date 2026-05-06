---
applyTo: "**"
---

# AIMathTutor — Agent Guide

## Build & Development

- **Primary interface:** `make` commands. Run `make help` for all targets.
- **Java 25 required.** `make check` enforces JDK 25 + Maven ≥3.9.9. CI uses Temurin 25.
- **Maven wrapper:** `./mvnw` (scripts fall back to system `mvn`).
- **Dev mode:** `make dev` → `quarkus:dev` on port `9001`. Dev UI: `http://localhost:9001/q/dev/`.
- **Tests:** `make test` → `./mvnw test`. Uses `@QuarkusTest`, Mockito, Panache Mock. CI runs `./mvnw verify`.
- **Install (skip tests):** `make install` → `./mvnw clean install -DskipTests`.
- **Production build:** Must pass `-Pproduction` for Vaadin `prepare-frontend` + `build-frontend`. CI: `./mvnw clean install package -DskipTests -Pproduction`.
- **JVM args required:** `--add-opens java.base/java.lang=ALL-UNNAMED` and `-XX:+EnableDynamicAgentLoading` (in `quarkus-maven-plugin`).
- **Versioning:** Maven property `${revision}` (default `1.0.0-SNAPSHOT`). Pass `-Drevision=X.Y.Z`.

## Architecture

- **Monolithic Quarkus 3.33 + Vaadin 25.** No REST boundary between views and services.
- **Base package:** `de.vptr.aimathtutor`. Views inject services via CDI (`@Inject`). REST clients are **only** for external AI APIs.
- **Packages:** `entity/` (Panache Active Record), `repository/`, `service/` (`@ApplicationScoped`), `view/` (Vaadin), `dto/`, `security/`, `event/`, `exception/`, `util/`, `component/`.
- **Graspable Math** workspace embedded via Vaadin + JavaScript API.

## Coding Conventions

- **Indentation:** 4 spaces. No tabs.
- **No FQCNs.** Always use imports. Enforced by Checkstyle `RegexpSinglelineJava`.
- **Logging:** Use `org.jboss.logging.Logger` (not SLF4J). Use `*f` methods (`infof`, `debugf`) with `%s` placeholders, not `*v` MessageFormat methods. Both enforced by Checkstyle.
- **ULIDs:** Use `UlidUtil`, never import `com.github.f4b6a3.ulid.UlidCreator` directly. Enforced by Checkstyle `IllegalImport`.
- **Vaadin UI threading:** Never block the UI thread. Use `CompletableFuture.supplyAsync()` + `ui.access()` + `.exceptionally()`:

```java
final var ui = getUI().orElse(null);
if (ui == null) return;
CompletableFuture.supplyAsync(blockingCall::get).thenAccept(result -> {
    ui.access(() -> { /* update UI */ });
}).exceptionally(ex -> {
    ui.access(() -> { /* show error */ });
    return null;
});
```

- **@Push:** Enabled globally on `AppConfig`. Views do not need their own `@Push`.
- **All `@Inject` fields in Vaadin views must be `transient`.** Vaadin serializes views.
- **In `onDetach(DetachEvent)`, use `detachEvent.getUI()` not `getUI()`.**

### Critical Anti-Patterns (Do Not Propose)

- **Do NOT make LoginView async.** `authService.authenticate()` in `CompletableFuture.supplyAsync()` causes `ContextNotActiveException` — `ui.access()` has no CDI request context and `MainLayout.beforeEnter()` needs EntityManager. Keep login synchronous.
- **CommentsPanel must NOT have `@Observes` methods.** Instantiated with `new`, not CDI. Real-time refresh uses `CommentCreatedEventBridge` with programmatic listeners.
- **ConversationContextDto fields must stay `private final` with unmodifiable getters.**
- **`VaadinSession.getCurrent()` can be null.** Always null-check. Applies to `AuthService.getUsername()`, `logout()`, `isAuthenticated()`.
- **MathWorkspaceView request ID staleness checks must stay.** `problemRequestId` counter, `pendingProblemFuture.cancel()`, and JS `window.currentProblemRequestId` prevent race conditions on rapid problem generation.
- **LoginAttemptServiceTest must verify exact cap value of 3600.** Do not revert to weak `<= 3600`.
- **RateLimitServiceTest must use `UUID.randomUUID()` for user IDs.** Hardcoded strings cause state leakage (`@ApplicationScoped`).
- **AdminConfigView save methods must null-check `authService.getUserId()`.** Use `requireUserId()` helper.
- **Security is session-based via `VaadinSession`, not Quarkus `SecurityIdentity`.** Permission checks via `PermissionService` in service layer. Do **not** add `@RolesAllowed` or `@Authenticated` to views. `MainLayout` and `AdminMainLayout` enforce auth via `BeforeEnterObserver`.

## Code Quality Gates

| Gate            | Command                                         | Notes                                         |
| --------------- | ----------------------------------------------- | --------------------------------------------- |
| Tests           | `make test`                                     | CI runs `./mvnw verify`                       |
| SpotBugs        | `./mvnw spotbugs:check`                         | Exclusions in `spotbugs-exclude.xml`          |
| Checkstyle      | `./mvnw checkstyle:check`                       | Google Java Style; config in `checkstyle.xml` |
| OWASP dep-check | `./mvnw org.owasp:dependency-check-maven:check` | Requires `NVD_API_KEY`; `failBuildOnCVSS=7`   |
| License report  | `./mvnw license:add-third-party`                | Runs at `verify` phase                        |

CI order: `test` → `security` (CodeQL) → `build` (package + SpotBugs + Checkstyle).

## Database

- **PostgreSQL.** Dev/test uses Quarkus devservices (`postgres:18.3-alpine3.23` on port `55432`).
- **Schema strategy:** Dev/Test = `drop-and-create` + `sql/init.sql`. Production = `validate` (schema must exist).
- **Test accounts:** `admin`/`admin`, `teacher`/`teacher`, `student1`/`student1`, `student2`/`student2`.
- **Password utility:** `make password` generates salt+hash for `init.sql`.

## AI Configuration

- **API keys (env vars):** `GEMINI_API_KEY`, `OPENAI_API_KEY`, `OPENAI_ORG_ID`.
- **Runtime settings (DB-backed):** Model, temperature, max tokens, prompts — configured via Admin Settings UI at `/admin/config`.
- **Mock provider:** `ai.tutor.provider=mock` or `ai.tutor.enabled=false`.
- **Test profile:** Disables `@Retry` delays on Ollama calls, sets 1s connect/read timeouts.

## Changelog

- Per-version files in `changelog/` (e.g., `changelog/2.2.5.md`).
- Follow [Keep a Changelog](https://keepachangelog.com). User-facing changes only, no class/method names.

## Docker

- **Production:** `docker-compose.yml` (app + PostgreSQL; optional pgadmin/Ollama).
- **Dockerfiles:** `src/main/docker/Dockerfile.alpine` and `Dockerfile.ubuntu` (port 9001, healthcheck `/q/health/ready`).
- **Build:** `scripts/build.sh` via `make build` — multi-platform `docker buildx` with QEMU fallback.
