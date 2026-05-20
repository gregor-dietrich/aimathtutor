---
applyTo: "**"
---

# AIMathTutor Project

## Important Note

You should challenge the user's request if it would result in implementing anti-patterns, security or performance issues, potential bugs, or if there are better alternatives, best practices, design choices, etc., that you recommend instead. You must follow the user's instructions if they disagree with you, however.

## Build & Development

- **Primary interface:** `make` commands. Run `make help` for all targets.
- **Java 25 required.** `make check` enforces JDK 25 + Maven ≥3.9.9. CI uses Temurin 25.
- **Maven wrapper:** `./mvnw` (scripts fall back to system `mvn`).
- **Dev mode:** `make dev` → `quarkus:dev` on port `9001`. Dev UI: `http://localhost:9001/q/dev/`.
- **Tests:** `make test` → `./mvnw verify`. Runs unit tests (skips integration tests). Uses `@QuarkusTest`, Mockito, Panache Mock.
- **Coverage:** `make coverage` → `scripts/coverage.sh`. Runs **all tests** (unit + integration tests via `-DskipITs=false`) with JaCoCo and generates a combined report.
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
| Tests           | `make test`                                     | CI runs `./mvnw verify -DskipITs=false` (unit + ITs); `make test` runs unit tests only                                                                |
| Coverage        | `make coverage`                                 | Runs all tests (including ITs) and generates report                                                                                                   |
| SpotBugs        | `./mvnw spotbugs:check`                         | Exclusions in `spotbugs-exclude.xml`                                                                                                                  |
| Checkstyle      | `./mvnw checkstyle:check`                       | Google Java Style; config in `checkstyle.xml`                                                                                                         |
| PMD             | `./mvnw pmd:check`                              | Unused code, complexity, style rules                                                                                                                  |
| CPD             | `./mvnw pmd:cpd-check`                          | Code duplication detection (DRY). Property `pmd-cpd.minTokens` in `pom.xml` (default 65). CLI override: `-Dpmd-cpd.minTokens=60`. Tokens ≈ lines × 6. |
| OWASP dep-check | `./mvnw org.owasp:dependency-check-maven:check` | Requires `NVD_API_KEY`; `failBuildOnCVSS=7`                                                                                                           |
| License report  | `./mvnw license:add-third-party`                | Runs at `verify` phase                                                                                                                                |

CI order: `test` → `security` (CodeQL) → `build` (package + SpotBugs + Checkstyle + PMD + CPD).

### ⚠️ Never Change Quality Gate Thresholds

**Never modify** any quality gate threshold, tolerance, or exclusion count in `pom.xml`, checkstyle, PMD, CPD, SpotBugs, or any other configuration. This includes, but is not limited to:

- `pmd-cpd.minTokens` (CPD minimum tokens)
- Checkstyle severity levels
- SpotBugs effort/maxRank
- PMD ruleset thresholds
- JaCoCo coverage limits

These thresholds are deliberately set by the project maintainers. Changing them to work around a code issue is strictly forbidden. Instead, refactor the code to pass the existing gates, but do so meaningfully, i.e. do not try to game detection by making meaningless changes - refactor properly instead. Also, Suppressions and Exclusions should be used as rarely as possible while being as fine-grained as possible.

## Database

- **PostgreSQL.** Dev/test uses Quarkus devservices (`postgres:18.4-alpine3.23` on port `55432`).
- **Schema strategy:** Dev/Test = `drop-and-create` + `sql/init.sql`. Production = `validate` (schema must exist).
- **Test accounts:** `admin`/`admin`, `teacher`/`teacher`, `student1`/`student1`, `student2`/`student2`.
- **Password utility:** `make password` generates salt+hash for `init.sql`.

## Encrypt-at-Rest

PII fields (currently `UserEntity.email`) are encrypted with AES-256-GCM at the JPA layer.

### Infrastructure

- **`EncryptionKeyManager`** (`service/security/`): loads/generates the 256-bit master key. Resolution order:
  1. `app.security.encryption-key-file` property (if set and non-empty — SmallRye Config will pick this up from env vars or properties)
  2. `$XDG_DATA_HOME/aimathtutor/encryption.key` if file exists
  3. `~/.aimathtutor/encryption.key` if file exists
  4. Auto-generate at the XDG path (creates dirs, sets permissions 600)
- **`EncryptionService`** (`service/security/`): derives two sub-keys from master via `HMAC(master, label)` (`"encrypt"` → AES key, `"blind-index"` → HMAC key). Provides `encrypt()`, `decrypt()`, and `generateBlindIndex()`.
- **`EncryptedStringConverter`** (`entity/converter/`): JPA `AttributeConverter` that calls `EncryptionService` via `CDI.current().select(EncryptionService.class).get()`.

### Ciphertext envelope format

`1|base64(iv)|base64(ciphertext+tag)` — version prefix `1` allows future key-rotation migrations.

### Searchable encrypted fields (blind index)

Encrypted columns cannot use SQL `LIKE`. Equality lookups use a companion `email_blind_index` column (`VARCHAR(44) UNIQUE`):

- Populated automatically in `UserRepository.persist()` via `encryptionService.generateBlindIndex(user.email)`.
- `findByEmailOptional()` queries by blind index, not by the encrypted column.
- Input is lowercased before hashing (`Locale.ROOT`) to support case-insensitive equality.
- Admin user search no longer includes email (LIKE on encrypted data is impossible) — username search only.

### Schema

- `email` column: `TEXT` (not `VARCHAR(255)`; encrypted envelope can reach ~380 chars).
- `email_blind_index`: `VARCHAR(44) UNIQUE`, indexed.

### PMD suppression

`EncryptionService.init()` carries `@SuppressWarnings("PMD.HardCodedCryptoKey")` — PMD false-positive on HKDF domain-separator strings (`"encrypt"`, `"blind-index"`). Suppression must stay as long as the derivation labels exist.

## AI Configuration

- **API keys:** `app.google.api.key`, `app.openai.api.key`, `app.openai.organization-id`.
- **SSRF protection:** `app.security.allowed-ollama-hosts` — comma-separated list of permitted hostnames for Ollama (defaults to `ollama,localhost`). Prevents SSRF via DNS rebinding.
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
- Named volume `aimathtutor_keys` mounted at `/etc/aimathtutor/keys`; property `app.security.encryption-key-file=/etc/aimathtutor/keys/encryption.key`.
- **Back up the key volume.** Losing the key makes all encrypted data permanently unrecoverable.
