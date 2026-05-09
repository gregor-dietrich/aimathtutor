---
applyTo: "**"
---

# AIMathTutor — Agent Guide

## Important Note

You should challenge the user's request if it would result in implementing anti-patterns, security or performance issues, potential bugs, or if there are better alternatives, best practices, design choices, etc., that you recommend instead. You must follow the user's instructions if they disagree with you, however.

## Build & Development

- **Primary interface:** `make` commands. Run `make help` for all targets.
- **Java 25 required.** `make check` enforces JDK 25 + Maven ≥3.9.9. CI uses Temurin 25.
- **Maven wrapper:** `./mvnw` (scripts fall back to system `mvn`).
- **Dev mode:** `make dev` → `quarkus:dev` on port `9001`. Dev UI: `http://localhost:9001/q/dev/`.
- **Tests:** `make test` → `./mvnw test`. Uses `@QuarkusTest`, Mockito, Panache Mock. CI runs `./mvnw verify`.
- **Install (skip tests):** `make install` → `./mvnw clean install -DskipTests`.
- **Lint:** `make lint` → `scripts/lint.sh` — runs compilation (Error Prone & NullAway), spotless:apply, checkstyle, spotbugs, PMD, and CPD checks.
- **Production build:** Must pass `-Pproduction` for Vaadin `prepare-frontend` + `build-frontend`. CI: `./mvnw clean install package -DskipTests -Pproduction`.
- **JVM args required:** `--add-opens java.base/java.lang=ALL-UNNAMED`, `--add-opens java.base/jdk.internal.ref=ALL-UNNAMED`, `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED`, `--add-opens java.base/java.nio=ALL-UNNAMED`, `--add-opens java.base/sun.nio.ch=ALL-UNNAMED`, `--enable-native-access=ALL-UNNAMED`, `--sun-misc-unsafe-memory-access=allow`, and `-XX:+EnableDynamicAgentLoading`. Set consistently in `pom.xml` (`quarkus-maven-plugin` `<jvmArgs>`), `.mvn/jvm.config`, and Docker `JAVA_OPTS_APPEND`.
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
- **Entity field `@Nullable` convention (NullAway-driven):** NullAway runs at ERROR level and treats unannotated fields as `@NonNull`. JPA entities use a no-arg constructor, so reference-type fields are null after construction before Hibernate populates them. Therefore **all entity fields** that are reference types (or collections) MUST be `@Nullable`, even if the DB column is `nullable=false`. Decision matrix:
  - **Primitives** (`boolean`, `int`, etc.): never `@Nullable`
  - **Auto-generated** (`id`, `version`, `publicId`, `created`, `lastEdit`): `@Nullable` — null before persist
  - **Collections** (`@OneToMany`, `@ManyToMany`): `@Nullable` — null before Hibernate wraps as PersistentBag
  - **Required business fields** (`username`, `title`, `content`, `name` with `nullable=false`): `@Nullable` — null after no-arg ctor, enforced at persist via `@NotBlank`/`@NotNull`
  - **Genuinely optional** (`email`, `avatarEmoji`, `parent`, moderation fields): `@Nullable`
  - **To-one associations** (`@ManyToOne`, `@OneToOne`): `@Nullable` even with `@JoinColumn(nullable=false)` — same null-after-ctor reason
  - **Pairing `@Nullable` + `@NotNull` (Bean Validation)** on the same field is valid: `@Nullable` placates NullAway, `@NotNull` rejects null at persist time

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

| Gate            | Command                                         | Notes                                                                                                                                                 |
| --------------- | ----------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| Lint (all)      | `make lint`                                     | Runs spotless:apply + checkstyle + spotbugs + PMD + CPD                                                                                               |
| Spotless        | `./mvnw spotless:apply`                         | Auto-formats code (included in `make lint`)                                                                                                           |
| Tests           | `make test`                                     | CI runs `./mvnw verify`                                                                                                                               |
| SpotBugs        | `./mvnw spotbugs:check`                         | Exclusions in `spotbugs-exclude.xml`                                                                                                                  |
| Checkstyle      | `./mvnw checkstyle:check`                       | Google Java Style; config in `checkstyle.xml`                                                                                                         |
| PMD             | `./mvnw pmd:check`                              | Unused code, complexity, style rules                                                                                                                  |
| CPD             | `./mvnw pmd:cpd-check`                          | Code duplication detection (DRY). Property `pmd-cpd.minTokens` in `pom.xml` (default 65). CLI override: `-Dpmd-cpd.minTokens=60`. Tokens ≈ lines × 6. |
| OWASP dep-check | `./mvnw org.owasp:dependency-check-maven:check` | Requires `NVD_API_KEY`; `failBuildOnCVSS=7`                                                                                                           |
| License report  | `./mvnw license:add-third-party`                | Runs at `verify` phase                                                                                                                                |

CI order: `test` → `security` (CodeQL) → `build` (package + SpotBugs + Checkstyle + PMD + CPD).

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
