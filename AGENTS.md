---
applyTo: "**"
---

# AIMathTutor Project

## Important Note

You should challenge the user's request if it would result in implementing
anti-patterns, security or performance issues, potential bugs, or if there are
better alternatives, best practices, design choices, etc., that you recommend
instead. You must follow the user's instructions if they disagree with you,
however.

## Agent Tooling

- **Claude Code uses the agent-kit plugin**, enabled with its marketplace in
  `.claude/settings.json`. The declaration does not install it; install it once
  per machine at user scope, as the plugin's README describes. It carries the
  generic agent method (delegation to pinned agents, review triage, debugging,
  design exploration, planning and multi-agent review skills) and guard hooks;
  for Claude Code that method is not repeated here.
- **Agents are agent-kit's** (`agent-kit:backend-developer`,
  `agent-kit:frontend-developer`, `agent-kit:code-reviewer`,
  `agent-kit:software-architect`, `agent-kit:scout`); the project defines none.
  Each reads this file and the scoped rules before acting.
- **Scoped rules** live in `.claude/rules/`: `ai-providers.md`, `vaadin-ui.md`,
  `test-conventions.md` and `docker-conventions.md`. Claude Code loads each for
  the paths its frontmatter names; other agents read the one for the area they
  touch.
- **`autoUpdate` is declared on** so nobody needs the `/plugin` toggle, which
  writes the setting into the first settings file that declares the marketplace
  and so would dirty this tracked one.
- **The marketplace is on a private forge.** A machine that can't reach it
  installs nothing from the declaration; no build, test or CI step depends on
  the plugin.

## Build & Development

- **Primary interface:** `make` commands. Run `make help` for all targets.
- **Java 25 required.** `make check` enforces JDK 25 + python3 ≥3.11, and checks
  the devkit parent POM; the parent's enforcer checks the JDK that Maven runs
  on. `make lint` also needs node ≥22.22.2 with npm (lint-md) and curl, tar and
  sha256sum/shasum (the gitleaks download). `devkitw` needs `ssh-keygen`
  (OpenSSH 8.1 or later) whenever it fetches a devkit release it has not cached;
  in a container, the running UID needs a passwd entry for it. CI uses Temurin
  25.
- **Maven:** every target that runs Maven, the project's own included, runs
  devkit's pinned Maven, downloaded and checksum-verified on first use into
  devkit's tools cache (`${XDG_CACHE_HOME:-~/.cache}/devkit/tools`; needs curl,
  tar, sha256sum/shasum); it never uses `mvn` on `PATH` or `./mvnw`. That Maven
  reads its own `conf/`, so a system Maven's global `settings.xml` does not
  apply under make; use `~/.m2/settings.xml`, or `-gs <file>` in
  `.mvn/maven.config`. `./mvnw` stays for IDE or bare use.
- **Dev mode:** `make dev` → `quarkus:dev` on port `9001`. Dev UI:
  `http://localhost:9001/q/dev/`. A production build leaves precompressed
  `.br`/`.gz` copies of the static CSS and JS in
  `target/classes/META-INF/resources/`, and dev mode serves those stale copies
  to browsers that accept them instead of the edited files; run `make clean`
  before checking CSS or JS changes in dev mode after a production build.
- **Tests:** `make test` → `make test-scripts` (python unittest of
  `scripts/tests`), then `mvn -q verify` with Quarkus console/file logging off.
  Runs unit tests (skips integration tests). Uses `@QuarkusTest` and Mockito;
  every `@QuarkusTest` gets the Dev Services database, so it needs Docker.
- **Coverage:** `make coverage` (devkit) writes `.coverage.md`. Runs **all
  tests** (unit + integration tests via `-DskipITs=false`) with JaCoCo and
  generates a combined report. The JaCoCo `report` goal is bound to
  `post-integration-test` so the report includes `*IT` coverage — surefire and
  failsafe both append to the same `target/jacoco.exec`, and a report generated
  earlier (at `test`) would silently omit every integration test.
- **Install (skip tests):** `make install` → `make check`, the
  `frontend-manifest` check, `mvn -q clean install -DskipTests`, then the
  frontend pin check. It builds from the committed
  `package.json`/`package-lock.json` and fails if either is missing; it never
  regenerates them.
- **Format:** `make format` — runs `format-md` (markdownlint fixes), then
  `spotless:apply` to auto-format code.
- **Lint:** `make lint` — runs `lint-repo` first (`lint-devkitw`, `lint-pins`,
  `lint-decisions`, `lint-secrets`, `lint-md`), then compilation (Error Prone &
  NullAway), spotless:check, checkstyle (shared and project rules), spotbugs,
  PMD, CPD and the frontend pin check (minimums in `devkit.toml`
  `[frontend.min-pins]`).
- **Audit:** `make audit` runs the OWASP dependency-check (`NVD_API_KEY` from
  the environment or gitignored `.env.build`).
- **Production build:** Must pass `-Pproduction` for Vaadin `prepare-frontend` +
  `build-frontend`. CI: `make install` with `MAVEN_ARGS=-Pproduction`.
  The pinned Maven's launcher reads `MAVEN_ARGS`, so this works through
  any devkit target (`./mvnw` ignores it).
- **JVM args required:** `--add-opens java.base/java.lang=ALL-UNNAMED`,
  `--add-opens java.base/jdk.internal.ref=ALL-UNNAMED`,
  `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED`,
  `--add-opens java.base/java.nio=ALL-UNNAMED`,
  `--add-opens java.base/sun.nio.ch=ALL-UNNAMED`,
  `--enable-native-access=ALL-UNNAMED`, `--sun-misc-unsafe-memory-access=allow`,
  and `-XX:+EnableDynamicAgentLoading`. Set consistently in `pom.xml`
  (`quarkus-maven-plugin` `<jvmArgs>`), `.mvn/jvm.config`, and Docker
  `JAVA_OPTS_APPEND`.
- **Node.js is pinned:** `vaadin.node.version` in `pom.xml` (Vaadin's default
  for the current release) is used by every frontend build path (production and
  regen builds, the Quarkus build step, dev mode). Vaadin downloads it to
  `~/.vaadin` and ignores any `node` on `PATH`, so `package-lock.json` comes out
  the same everywhere. Bump it with `vaadin.version`.
- **Frontend installs use `npm ci`** (`vaadin.ci.build` in `pom.xml`): builds
  install exactly what `package-lock.json` records and fail when `package.json`
  disagrees with it, e.g. after changing an `@NpmPackage`. Fix that with
  `make regen-frontend` (the only path that runs `npm install`, besides dev
  mode's runtime dev server, which Vaadin can't switch to `npm ci`), then commit
  the regenerated manifest.
- **Versioning:** Maven property `${revision}` (default `1.0.0-SNAPSHOT`). Pass
  `-Drevision=X.Y.Z`.

### Shared tooling (devkit)

- **Pinned, not copied:** the shared Make targets, scripts,
  Checkstyle/PMD/formatter configs and the parent POM
  (`de.vptr.devkit:devkit-parent`, which configures and activates every Maven
  gate plugin and pins their versions) come from devkit, pinned by `version` and
  `commit` in `devkit.toml`. `./devkitw` fetches that pin into a read-only
  per-user cache, after verifying the release tag's SSH signature and that the
  commit is on devkit's `main`, and links it as `.devkit`. Any `make` target
  creates the link; a bare `./mvnw` or an IDE import fails on a fresh clone
  because Maven cannot resolve the parent POM without the link. The pom
  therefore declares no repository besides Central: any other repository would
  be asked for the parent.
- **Never edit `.devkit` or `devkitw`.** A shared gate changes in devkit and is
  released there. To adopt a release, edit `version` and `commit` in
  `devkit.toml` together with the root pom's `<parent><version>` (the tag
  without the `v`), and copy the new `devkitw` when the release changed it
  (`lint-devkitw` fails until it is copied), then re-run `make hooks`. Follow
  the upgrade notes in `.devkit/README.md` for every release the bump crosses.
  Dependabot cannot bump the pin, so bumps are by hand.
- **Project values stay here:** `pom.xml` keeps overrides of the parent's
  properties (`jacoco.check.*`, `nullaway.annotated.packages`), the project's
  own plugin configuration (e.g. JaCoCo excludes) and names appended to the
  parent's license allowlist (the `add-third-party` execution,
  `combine.children="append"`). Beside it are `checkstyle-project.xml` (the
  project's own Checkstyle rules, run as checkstyle execution `project`), and
  the suppression/exclusion files (`checkstyle-suppressions.xml`,
  `spotbugs-exclude.xml`, `dependency-check-suppression.xml`).
- **Hooks:** run `make hooks` once per clone: pre-commit runs `make lint-repo`,
  pre-push runs `make lint-repo test`. `make gate` runs the push gate and
  records a passing clean HEAD, so the next push skips it.

## Architecture

- **Monolithic Quarkus 3.40 + Vaadin 25.** No REST boundary between views and
  services.
- **Base package:** `de.vptr.aimathtutor`. Views inject services via CDI
  (`@Inject`). REST clients are **only** for external AI APIs.
- **Packages:** `entity/` (Panache Active Record), `repository/`, `service/`
  (`@ApplicationScoped`), `view/` (Vaadin), `dto/`, `security/`, `event/`,
  `exception/`, `util/`, `component/`.
- **Graspable Math** workspace embedded via Vaadin + JavaScript API.
- **Appearance:** each user's design and colour mode are `users.design` and
  `users.color_mode` (`Appearance.Design`/`ColorMode`, stored as enum names).
  - `AuthService` loads them into the session at login and clears them with the
    other sign-in attributes.
  - `ThemeService` renders the session's appearance into every bootstrap page
    through an `IndexHtmlRequestListener`, as `data-design`, `data-base` and
    `data-color-mode` on `<html>`. It also removes whichever base-theme link is
    not in use (`AppConfig` declares Lumo and Aura).
  - `js/appearance.js`, inlined into `<head>`, sets `data-color-scheme`
    (`light`/`dark`) and follows the OS for "Follow system".
  - Login, logout and saving in Settings reload the page; only the top-bar
    toggle switches the colour mode live.

## Coding Conventions

- **Indentation:** 4 spaces. No tabs.
- **No FQCNs.** Always use imports. Enforced by Checkstyle
  `RegexpSinglelineJava`.
- **Logging:** Use `org.jboss.logging.Logger` (not SLF4J). Use `*f` methods
  (`infof`, `debugf`) with `%s` placeholders, not `*v` MessageFormat methods.
  Both enforced by Checkstyle.
- **ULIDs:** Use `UlidUtil`, never import `com.github.f4b6a3.ulid.UlidCreator`
  directly. Enforced by Checkstyle `IllegalImport`.
- **Vaadin UI threading:** Never block the UI thread. Load view data through
  `AsyncDataLoader.load(...)` (`util/`), which runs the call off the UI thread
  with a timeout and applies the result or the error in `ui.access()`. Where it
  doesn't fit, use `CompletableFuture.supplyAsync()` + `ui.access()` +
  `.exceptionally()`:

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

- **@Push:** Enabled globally on `AppConfig`. Views do not need their own
  `@Push`.
- **Theme-neutral styling:** the app runs on Lumo or Aura, so Java names no
  base-theme token or variant. Only `AppConfig` (which stylesheets load) and
  `ThemeService` (which base link a page keeps) name a theme, through the
  `Lumo` and `Aura` classes; `Appearance.Base` names them only as `data-base`
  values.
  - Inline styles use Vaadin's theme-neutral properties:
    `--vaadin-text-color[-secondary]`, `--vaadin-background-color`,
    `--vaadin-background-container[-strong]`,
    `--vaadin-border-color[-secondary]`, `--vaadin-radius-*`,
    `--vaadin-padding-*` for padding, and `--vaadin-gap-*` for margins and
    gaps.
  - Accent and status colours have no neutral property, so they use the
    `--app-*` tokens through `AppStyles`.
  - Variants use the neutral constants (`ButtonVariant.PRIMARY`, not
    `LUMO_PRIMARY`); badges are `Badge` with `BadgeVariant`.
  - Styling shared by two or more components is a class in `styles.css`,
    named in `AppStyles`.
  - Checkstyle `RegexpSinglelineJava` `BaseThemeCoupling` rejects `--lumo-*`,
    `--aura-*` (also their private `--_lumo-*`/`--_aura-*`), `LUMO_*` and
    `AURA_*` in Java.
- **The base-theme bridge** at the top of `styles.css` and the design
  stylesheets are the only places `--lumo-*` and `--aura-*` appear.
  - Each `--app-*` token takes Lumo's value and falls back to Aura's, so it
    follows whichever base stylesheet is loaded. Never load both.
  - Every `var()` in its `:root` tokens is substituted on `<html>`, where the
    app sets the colour scheme. They would still follow one set lower down,
    because both themes write those colours as `light-dark()` on `:root`, which
    an unregistered token carries unresolved to the element that uses it.
  - So they reference only `--lumo-*`/`--aura-*`, never `--vaadin-*` colours:
    Vaadin registers six of those as `<color>`, which fixes them to one colour
    on `<html>`. Read a `--vaadin-*` colour where it is used.
  - `AppStylesTest` pins every `AppStyles` name against the stylesheet.
- **Designs** are one stylesheet each under `META-INF/resources/designs/`,
  keyed on `html[data-design]` (Vellum's contrast badge keys on
  `html[data-base="aura"]`); the Lumo designs feed private `--design-*`
  inks to `lumo-inks.css`, and an Aura design never sets `--lumo-*`.
  - Their fonts are self-hosted under `designs/fonts/<family>/` with that
    family's `OFL.txt`; `designs/fonts.css` records each file's source and
    version.
  - Rules that style Vaadin component internals (host box model, `::part`) go
    only in `designs/component-tweaks.css`.
- **All `@Inject` fields in Vaadin views must be `transient`.** Vaadin
  serializes views.
- **In `onDetach(DetachEvent)`, use `detachEvent.getUI()` not `getUI()`.**
- **To-one associations are lazy:** `@ManyToOne(fetch = FetchType.LAZY)`, as
  every existing one is. Fetch eagerly only where profiling justifies it.
- **Passwords** are hashed and verified only through `PasswordHashingService`
  (bcrypt). bcrypt reads at most 72 bytes, so `UserService` and
  `PasswordHashingService` refuse a password over 72 UTF-8 bytes, and `UserDto`
  caps it at 72 characters (`AppConstants.PASSWORD_MAX_LENGTH`, pinned by
  `PasswordSizeConstraintTest`).
- **Entity field `@Nullable` convention (NullAway-driven):** NullAway runs at
  ERROR level and treats unannotated fields as `@NonNull`. JPA entities use a
  no-arg constructor, so reference-type fields are null after construction
  before Hibernate populates them. Therefore **all entity fields** that are
  reference types (or collections) MUST be `@Nullable`, even if the DB column is
  `nullable=false`. Decision matrix:
  - **Primitives** (`boolean`, `int`, etc.): never `@Nullable`
  - **Auto-generated** (`id`, `version`, `publicId`, `created`, `lastEdit`):
    `@Nullable` — null before persist
  - **Collections** (`@OneToMany`, `@ManyToMany`): `@Nullable` — null before
    Hibernate wraps as PersistentBag
  - **Required business fields** (`username`, `title`, `content`, `name` with
    `nullable=false`): `@Nullable` — null after no-arg ctor, enforced at persist
    via `@NotBlank`/`@NotNull`
  - **Genuinely optional** (`email`, `avatarEmoji`, `parent`, moderation
    fields): `@Nullable`
  - **To-one associations** (`@ManyToOne`, `@OneToOne`): `@Nullable` even with
    `@JoinColumn(nullable=false)` — same null-after-ctor reason
  - **Enums with a Java initializer** (`CommentEntity.status`,
    `UserEntity.design`, `UserEntity.colorMode`): not `@Nullable`. The
    initializer covers `new` and the column `DEFAULT` covers SQL inserts; keep
    them equal (`Appearance.DEFAULT`, pinned by `UserServiceTest`).
  - **Pairing `@Nullable` + `@NotNull` (Bean Validation)** on the same field is
    valid: `@Nullable` placates NullAway, `@NotNull` rejects null at persist
    time

### Critical Anti-Patterns (Do Not Propose)

- **Do NOT make LoginView async.** `authService.authenticate()` in
  `CompletableFuture.supplyAsync()` causes `ContextNotActiveException` —
  `ui.access()` has no CDI request context and `MainLayout.beforeEnter()` needs
  EntityManager. Keep login synchronous.
- **CommentsPanel must NOT have `@Observes` methods.** Instantiated with `new`,
  not CDI. Real-time refresh uses `CommentCreatedEventBridge` with programmatic
  listeners.
- **ConversationContextDto fields must stay `private final` with unmodifiable
  getters.**
- **`VaadinSession.getCurrent()` can be null.** Always null-check. Applies to
  `AuthService.getCurrentUserEntity()`, `currentSessionCredentials()`,
  `renewCredentialStamp()`, `logout()`, `isAuthenticated()`.
- **MathWorkspaceView request ID staleness checks must stay.**
  `problemRequestId` counter, `pendingProblemFuture.cancel()`, and JS
  `window.currentProblemRequestId` prevent race conditions on rapid problem
  generation.
- **LoginAttemptServiceTest must verify exact cap value of 3600.** Do not revert
  to weak `<= 3600`.
- **RateLimitServiceTest must use `UUID.randomUUID()` for user IDs.** Hardcoded
  strings cause state leakage (`@ApplicationScoped`).
- **AdminConfigView save methods must null-check `authService.getUserId()`.**
  Use `requireUserId()` helper.
- **Do NOT move `ProductionProfileGuard` or `SchemaManagementGuard` to
  `StartupEvent`**, even though every build logs a Quarkus warning recommending
  it. Hibernate's schema management runs before `StartupEvent`, so a
  `StartupEvent` guard would run after the tables are gone. Both guards observe
  `@Initialized(ApplicationScoped.class)`, which fires during static init;
  `ProductionProfileGuardTest` and `SchemaManagementGuardTest` pin that.
- **`html[data-color-scheme]` is the only dark-mode signal.** CSS and JS key on
  `html[data-color-scheme="dark"]`. Never set a `theme` attribute for colour,
  on `<html>` or the UI's `<body>`, and never call `Page.setColorScheme`: it
  overwrites `<html theme>` and enables Lumo's legacy `[theme~=dark]` block,
  which overrides the designs.
  - **Exception:** Graspable Math's own elements (`#gm-keyboard`, the help
    button and dialog) key on `html.gm-dark-theme`, the class gmath sets itself.
    gmath 3.5.16 cannot switch from dark back to light (`setDarkTheme(false)`
    throws and the class stays), so these overrides follow gmath, not the
    scheme, and the workspace stays dark until the next page load.
- **Never load Lumo and Aura together.** `styles.css`'s `--app-*` tokens
  resolve as `var(--lumo-…, <aura>)`, so with both loaded they silently take
  Lumo's values. The base theme changes only between page loads.
- **Do not apply the appearance in `beforeEnter` or after attach.** Eager
  server load is off, so first paint happens before view code runs; only the
  bootstrap listener avoids a flash of the default look.
- **Security is session-based via `VaadinSession`, not Quarkus
  `SecurityIdentity`.** Permission checks via `PermissionService` in service
  layer. Do **not** add `@RolesAllowed` or `@Authenticated` to views.
  `MainLayout` and `AdminMainLayout` enforce auth via `BeforeEnterObserver`.
- **The session holds the user's `publicId` and a credential stamp, never the
  username.** Resolve the current user through
  `AuthService.getCurrentUserEntity()`, which refuses a session whose stamp no
  longer matches the password hash, so a password change ends the account's
  other sessions; off-UI-thread work takes
  `AuthService.currentSessionCredentials()` captured on the UI thread and
  verifies them in its own transaction, as `UserService.changePassword` does.
  Only `changePassword` changes the caller's own password, after the
  current-password check and its per-session attempt limit; admin write paths
  refuse it with `AppConstants.OWN_PASSWORD_MESSAGE`. Views that cache a user ID
  at navigation still act on it until their next navigation. Every user write
  that can end sessions fires `UserAccountChangedEvent`; `AuthService` evicts
  its cache in an `AFTER_SUCCESS` observer, never before commit.
- **Every user or rank write path must enforce the privilege ceiling.** Read the
  caller's permissions with `UserRankService.requireCallerPermissions()` before
  writing (before editing a rank, which may be the caller's own), and call
  `UserRankService.requireWithin()` on every rank the write touches, assigns or
  produces. A caller may never create, change, assign or delete a user or rank
  holding a permission their own rank lacks.
- **Every user or rank write path a caller triggers must keep an active
  administrator** (activated, unbanned, rank holding every permission;
  `UserRepository.isActiveAdministrator`). A path that can remove one
  (`updateUser`, `patchUser`, `deleteUser`, `updateRank`, `patchRank`) calls
  `UserRepository.lockAdministrators()` first, before its permission check loads
  the caller and before it loads the target, so concurrent writes can't each
  count the other's administrator; this relies on READ COMMITTED, which
  `application.properties` pins. Take the before-state before modifying
  anything, as `UserService.saveUpdatedUser`/`deleteUser` and
  `UserRankService.holdsLastAdministrators` do, and refuse with
  `AppConstants.LAST_ADMINISTRATOR_MESSAGE`.

## Code Quality Gates

| Gate              | Command                                          | Notes                                                                                                                                                              |
| ----------------- | ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Repository        | `make lint-repo`                                 | `lint-devkitw` + `lint-pins` + `lint-decisions` + `lint-secrets` (gitleaks) + `lint-md` (markdownlint); `make format-md` fixes Markdown; `make lint` runs it first |
| Lint (all)        | `make lint`                                      | Runs lint-repo, then spotless:check + checkstyle + spotbugs + PMD + CPD + frontend pin check                                                                       |
| Spotless          | `./mvnw spotless:check`                          | Enforces code formatting (runs at `verify` phase); use `make format` to fix                                                                                        |
| Tests             | `make test`                                      | CI runs `make test` with `MAVEN_ARGS=-DskipITs=false` (unit + ITs); `make test` alone runs unit tests only                                                         |
| Coverage          | `make coverage`                                  | Runs all tests (including ITs) and generates report                                                                                                                |
| SpotBugs          | `./mvnw spotbugs:check`                          | Exclusions in `spotbugs-exclude.xml`                                                                                                                               |
| Checkstyle        | `make lint`                                      | Google Java Style; shared config from devkit, project rules in `checkstyle-project.xml`; alone: `./mvnw checkstyle:check checkstyle:check@project`                 |
| PMD               | `./mvnw initialize pmd:check`                    | Unused code, complexity, style rules                                                                                                                               |
| CPD               | `./mvnw pmd:cpd-check`                           | Code duplication detection (DRY). Property `pmd-cpd.minTokens` (devkit parent default 65). CLI override: `-Dpmd-cpd.minTokens=60`. Tokens ≈ lines × 6.             |
| OWASP dep-check   | `make audit`                                     | Not bound to a phase; CI `security` job runs it. `failBuildOnCVSS=7`. Needs `NVD_API_KEY` from environment or gitignored `.env.build` (not `.env`)                 |
| License allowlist | `./mvnw license:add-third-party@add-third-party` | Runs at `verify`; fails on a dependency whose license the parent's allowlist and `pom.xml`'s appends don't name (`mvn verify` without `-q` lists each)             |

CI order: `test` (`make -k lint-repo`, so every repo gate runs even if one
fails, then `make test-scripts`, then `make -o test-scripts test` with
`MAVEN_ARGS=-DskipITs=false`; the `-o` skips the prerequisite already run, so a
failing script test doesn't stop the Java tests; both run regardless of the
gate's result) and `security` (CodeQL around `make install`, then `make audit`
when `NVD_API_KEY` is set; only that step receives the key) run in parallel,
then `build` (`make install` with `MAVEN_ARGS=-Pproduction`: package, spotless,
SpotBugs, Checkstyle, PMD, CPD). A push-only
`dependency-submission` job submits the Maven dependency graph after
`./devkitw path`. Secret scanning is `lint-secrets`, not a separate action.

- **Compiler warnings are build failures.** `maven-compiler-plugin`, configured
  in devkit's parent POM, passes `-Werror` and
  `-Xlint:all,-serial,-this-escape,-classfile`, so every javac lint warning and
  every Error Prone warning (any severity) fails compilation. The three excluded
  lint categories are deliberate and documented in the parent POM; do not
  exclude further categories to work around a warning — fix the code.
- **Intentional build warning (do not fix):** `[WARNING]
  [io.quarkus.arc.deployment.ObserverValidationProcessor] The method
  de.vptr.aimathtutor.ProductionProfileGuard#checkProfiles is an observer for
  @Initialized(ApplicationScoped.class) ... We strongly recommend to observe
  StartupEvent instead`. The same warning is logged for
  `SchemaManagementGuard#checkStrategy`. See the `ProductionProfileGuard`
  anti-pattern above.

### ⚠️ Never Change Quality Gate Thresholds

**Never modify** any quality gate threshold, tolerance, or exclusion count in
`pom.xml`, `devkit.toml`, `checkstyle-project.xml`, the suppression files, or
any other configuration. The thresholds live in devkit's parent POM and shared
configs plus this pom's override properties (`jacoco.check.*`); never change
either here, and a shared value changes only through a devkit release. This
includes, but is not limited to:

- `pmd-cpd.minTokens` (CPD minimum tokens)
- Checkstyle severity levels
- SpotBugs effort/maxRank
- PMD ruleset thresholds
- JaCoCo coverage limits

These thresholds are deliberately set by the project maintainers. Changing them
to work around a code issue is strictly forbidden. Instead, refactor the code to
pass the existing gates, but do so meaningfully, i.e. do not try to game
detection by making meaningless changes - refactor properly instead. Also,
Suppressions and Exclusions should be used as rarely as possible while being as
fine-grained as possible.

Not threshold changes: bumping the devkit pin (`[devkit]` `version`/`commit`
with the parent `<version>`) and changing `[frontend.min-pins]`, each as its
issue directs (e.g. #165, #207). Appending to `pom.xml`'s license allowlist a
spelling of a license the parent already allows is not one either; admitting a
new license (as UPL-1.0 was) needs the maintainer's approval.

## Database

- **PostgreSQL.** Dev/test uses Quarkus devservices (`postgres:18.6-alpine3.24`,
  dev on port `55432`, test on a random port).
- **Schema strategy:** Flyway owns the schema in all profiles. Hibernate is
  `validate`-only. Migrations live in `src/main/resources/db/migration`, and
  dev/test demo data lives in `db/demo/R__demo_data.sql`. Profiles are picked at
  runtime, so `ProductionProfileGuard` refuses a production launch
  (`LaunchMode.NORMAL`) with a dev/test profile before Flyway and Hibernate
  start. `SchemaManagementGuard` refuses one whose Hibernate schema action is
  anything but `none`/`validate` under the names it checks:
  `schema-management.strategy`, the deprecated `database.generation`, and
  `jakarta.persistence.schema-generation.database.action` and
  `hibernate.hbm2ddl.auto` via `unsupported-properties`, each under the plain,
  `"<default>"` and `<default>` persistence-unit names. A new name Hibernate
  takes the schema action from needs adding there.
- **Test accounts:** `admin`/`admin`, `teacher`/`teacher`,
  `student1`/`student1`, `student2`/`student2`. Production seeds no accounts: on
  startup in `LaunchMode.NORMAL`, `AppLifecycleBean` creates the first admin
  from `app.bootstrap.admin-username`/`-password` when `users` is empty, and
  replaces the password of a 4.x-seeded `admin` that still accepts `admin`. Both
  fail startup when no password is configured. 4.x-seeded
  `teacher`/`student1`/`student2` that still accept their published passwords
  are deactivated on every production start.
- **Password utility:** `make password` generates a bcrypt hash for seed data or
  an administrator reset (README).

### Migrations

- Name files `V<n>__<snake_case>.sql`, with `n` = the next integer.
- **Never edit a migration that has been merged to main**, because checksum
  validation fails on every deployed DB. Fix it with a new migration.
- Every entity change that alters the schema ships with its migration in the
  same PR.
- Indexes and constraints are declared only in migrations, never via
  `@Table(indexes/uniqueConstraints)`.
- Every foreign key needs an index whose leading columns are the key's columns.
  `ForeignKeyIndexIT` enforces this; it runs with the integration tests, not
  under `make test`.
- Update `R__demo_data.sql` when the migration touches seeded tables.
- Migrations must be safe on a populated production DB. For example, a new
  `NOT NULL` column needs a `DEFAULT` or a backfill.
- Enum names stored in a column are data: renaming or removing a constant needs
  a migration that rewrites those rows first. Hibernate refuses to load a row
  with an unknown name, and for `users` that blocks the login.

## Encrypt-at-Rest

PII fields (currently `UserEntity.email`) are encrypted with AES-256-GCM at the
JPA layer.

### Infrastructure

- **`EncryptionKeyManager`** (`service/security/`): loads/generates the 256-bit
  master key. Resolution order:
  1. `app.security.encryption-key-file` property (if set and non-empty —
     SmallRye Config will pick this up from env vars or properties)
  2. `$XDG_DATA_HOME/aimathtutor/encryption.key` if file exists
  3. `~/.aimathtutor/encryption.key` if file exists
  4. Auto-generate at the XDG path (creates dirs, sets permissions 600)
- **`EncryptionService`** (`service/security/`): derives two sub-keys from
  master via `HMAC(master, label)` (`"encrypt"` → AES key, `"blind-index"` →
  HMAC key). Provides `encrypt()`, `decrypt()`, and `generateBlindIndex()`.
- **`EncryptedStringConverter`** (`entity/converter/`): JPA `AttributeConverter`
  that calls `EncryptionService` via
  `CDI.current().select(EncryptionService.class).get()`.

### Ciphertext envelope format

`1|base64(iv)|base64(ciphertext+tag)` — version prefix `1` allows future
key-rotation migrations.

### Searchable encrypted fields (blind index)

Encrypted columns cannot use SQL `LIKE`. Equality lookups use a companion
`email_blind_index` column (`VARCHAR(44) UNIQUE`):

- Populated automatically in `UserRepository.persist()` via
  `encryptionService.generateBlindIndex(user.email)`.
- `findByEmailOptional()` queries by blind index, not by the encrypted column.
- Input is lowercased before hashing (`Locale.ROOT`) to support case-insensitive
  equality.
- Admin user search no longer includes email (LIKE on encrypted data is
  impossible) — username search only.

### Schema

- `email` column: `TEXT` (not `VARCHAR(255)`; encrypted envelope can reach ~380
  chars).
- `email_blind_index`: `VARCHAR(44) UNIQUE`, indexed.

### PMD suppression

`EncryptionService.init()` carries `@SuppressWarnings("PMD.HardCodedCryptoKey")`
— PMD false-positive on HKDF domain-separator strings (`"encrypt"`,
`"blind-index"`). Suppression must stay as long as the derivation labels exist.

## AI Configuration

- **API keys:** `app.google.api.key`, `app.openai.api.key`,
  `app.openai.organization-id`.
- **SSRF protection:** `app.security.allowed-ollama-hosts` — comma-separated
  list of permitted hostnames for Ollama (defaults to `ollama,localhost`).
  Prevents SSRF via DNS rebinding.
- **Runtime settings (DB-backed):** Model, temperature, max tokens, prompts —
  configured via Admin Settings UI at `/admin/config`.

- **Mock provider:** `ai.tutor.provider=mock` or `ai.tutor.enabled=false`.
- **Test profile:** Disables `@Retry` delays on AI provider calls, sets 1s
  connect/read timeouts for Ollama.

## Changelog

- Per-version files in `changelog/` (e.g., `changelog/2.2.5.md`).
- Follow [Keep a Changelog](https://keepachangelog.com). User-facing changes
  only, no class/method names.

## Docker

- **Production:** the compose file in `docs/QUICKSTART.md`, which runs the
  published image. The root `docker-compose.yml` (app + PostgreSQL; optional
  pgadmin/Ollama) builds the app from the checkout (`pull_policy: build`) and is
  for development only.
- **Dockerfiles:** `src/main/docker/Dockerfile.alpine` and `Dockerfile.ubuntu`
  (port 9001, healthcheck `/q/health/ready`).
- **Build:** `scripts/build.sh` via `make build` — host-platform images into the
  local image store (`docker buildx build --load`, or plain `docker build`
  without buildx).
- **Release:** `scripts/release.sh` via `make release` — one `docker buildx
  build --platform linux/amd64,linux/arm64 --push` per Dockerfile with all of
  its tags; nothing is pushed from the local image store. It refuses up front any
  version `make tag` would refuse (anything but `X.Y.Z`, optionally prefixed `v`
  and followed by `rcN` or `-rcN`, which covers `-SNAPSHOT`), and fails before
  testing unless the current buildx builder lists both platforms (the `docker`
  driver also needs the containerd image store). It also refuses a
  `package.json` or `package-lock.json` that differs from HEAD, both before the
  tests and again right before packaging, so the images ship the committed
  lockfile (`scripts/lib/frontend.sh`). A cache-only multi-platform build of
  each Dockerfile runs before the git tag, so only a push can fail
  after it. Helpers shared with `make build` live in `scripts/lib/images.sh`.
- Named volume `aimathtutor_keys` mounted at `/etc/aimathtutor/keys`; property
  `app.security.encryption-key-file=/etc/aimathtutor/keys/encryption.key`.
- **Back up the key volume.** Losing the key makes all encrypted data
  permanently unrecoverable.
