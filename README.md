# AIMathTutor

AIMathTutor is a full-stack web application for interactive math learning, built with Quarkus (backend) and Vaadin (frontend). It features an embedded Graspable Math workspace, AI-powered tutoring, lesson/exercise management, analytics, and granular user roles.

## 🌟 Features

- Interactive Graspable Math workspace for symbolic manipulation and step-by-step actions
- Real-time AI tutor feedback, hints, and adaptive problem generation (Google, OpenAI, Ollama, mock)
- Problem and lesson authoring, organization, and progress tracking
- Threaded comments on exercises, moderation, and reporting
- Session/event tracking and analytics dashboards for teachers/admins
- Granular user management: users, groups, ranks, and permissions
- Tight Quarkus + Vaadin integration: CDI-injected services, no REST boundary for core logic

## 🚀 Getting Started

See [Quickstart](docs/QUICKSTART.md) for setup and usage.

### Deployment

When deploying to production, it is **critical** to override the default database password. Set the `QUARKUS_DATASOURCE_PASSWORD` environment variable to a strong password to replace the default `changeit` value used in dev/test profiles.

Never set a `dev` or `test` profile (`QUARKUS_PROFILE`, `QUARKUS_CONFIG_PROFILE_PARENT`) on a production deployment: those profiles drop and recreate the database tables, so the app refuses to start with one. Likewise, never override Hibernate's schema management (`QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY`, the deprecated `QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION`, or their `quarkus.hibernate-orm...` property forms): anything but `validate` or `none` makes the app refuse to start. In both cases the reason appears in `docker compose logs app`.

### Recovering administrator access

The app refuses to delete, ban, deactivate or demote its last active administrator. If no administrator can log in anyway (for example after a forgotten password), reset one directly in the database. You need a checkout of this repository and JDK 25 (`./mvnw` fetches Maven); the checkout you run `docker compose` from will do.

1. Run `make password` and copy the printed `hash=` value. Choose a password that meets the app's rules, which `make password` doesn't enforce: 8 to 72 characters, with an uppercase and a lowercase letter, a digit and a symbol.
2. Open psql in the database container. These are the `docker-compose.yml` defaults; use your values if you set `SQL_USERNAME` or `SQL_DATABASE`:

   ```sh
   docker compose exec db psql -U aimathtutor -d aimathtutor
   ```

3. List the users holding the Admin rank. Restoring its permissions makes every active one of them an administrator, so review the list, especially after a compromise:

   ```sql
   SELECT u.username, u.activated, u.banned FROM users u JOIN user_ranks r ON r.id = u.rank_id
     WHERE r.public_id = '01ARZ3NDEKTSV4RRFFQ69G5FAV';
   ```

4. Restore the Admin rank's administration permissions and reset the account (replace `<hash>` and `<name>`; usernames are stored in lower case):

   ```sql
   BEGIN;
   UPDATE user_ranks SET admin_view = TRUE, user_edit = TRUE, user_rank_edit = TRUE
     WHERE public_id = '01ARZ3NDEKTSV4RRFFQ69G5FAV';
   UPDATE users SET password = '<hash>', banned = FALSE, activated = TRUE,
     rank_id = (SELECT id FROM user_ranks WHERE public_id = '01ARZ3NDEKTSV4RRFFQ69G5FAV')
     WHERE username = '<name>';
   ```

   Run `COMMIT;` only if the users `UPDATE` reported `UPDATE 1`; otherwise run `ROLLBACK;` and fix the name. The hash contains `$`, so type it inside psql or single quotes, never inside a double-quoted shell string, where the shell would expand it.

   If the Admin rank was deleted, the rank `UPDATE` reports `UPDATE 0` and the users `UPDATE` fails on a NULL `rank_id`. Run `ROLLBACK;`, pick another rank from `SELECT public_id, name FROM user_ranks;`, and repeat steps 3 and 4 with its `public_id`.

5. Restart the app with `docker compose restart app`. This step is required: it clears the failed-login lockouts that the forgotten password has probably triggered, ends every open session (a password change alone doesn't), and drops the cached rank list, so the Ranks page doesn't show, and re-save, the old permissions.

If logging in fails with a server error after the reset, check that the app still mounts its original encryption key volume: a reset doesn't help when the key is lost.

### Common Development Commands (via Makefile)

- `make dev` – Start Quarkus in dev mode
- `make test` – Execute unit tests (skips integration tests)
- `make coverage` – Execute all tests (unit + integration) and generate JaCoCo report
- `make build` – Build the Docker images for the local image store (`make check`, `mvn package`, native-platform `docker build`)
- `make install` – `make check` and `mvn clean install -DskipTests`
- `make password` – Generate a bcrypt hash for a password (for init.sql or an administrator reset)
- `make release` – Pull from origin/main, test, `make tag`, and build and push multi-platform Docker images with `docker buildx`
- `make branch`, `make tag`, `make rebase`, `make untag` – Git branch/tag management

See the [Makefile](Makefile) or use `make help` for all available commands and scripts.

## 🤖 Supported AI Providers

- [Google](https://aistudio.google.com/api-keys)
- [Ollama](https://ollama.com/download)
- [OpenAI](https://platform.openai.com/api-keys)

**Configuration:**

- **API Keys**: Set properties `app.google.api.key`, `app.openai.api.key`, and `app.openai.organization-id` (immutable at runtime).
- **Provider Settings** (model, base URL, temperature, prompts, etc.): Configure via the **Admin Settings UI** at `/admin/config` after login (runtime-mutable, database-backed).
- **Encryption key**: Resolution order: (1) `app.security.encryption-key-file` property, (2) `$XDG_DATA_HOME/aimathtutor/encryption.key`, (3) `~/.aimathtutor/encryption.key`, (4) auto-generate a 256-bit key at the XDG path with 0600 permissions. In Docker, mount the `aimathtutor_keys` volume at `/etc/aimathtutor/keys`. This key encrypts PII fields (such as user email) at rest.

See [docs/QUICKSTART.md](docs/QUICKSTART.md) and [docs/BUILD_GUIDE.md](docs/BUILD_GUIDE.md) for detailed setup instructions.

## 📖 Documentation

- [Quickstart](docs/QUICKSTART.md)
- [Build Guide](docs/BUILD_GUIDE.md)
- [Project Instructions](AGENTS.md)

## 🛠️ Project Structure & Workflow

- Monolithic Quarkus + Vaadin app
- Vaadin views inject backend services via CDI (`@Inject`)
- Graspable Math workspace embedded via Vaadin and JavaScript API
- AI Tutor layer supports Google, OpenAI, Ollama, and mock providers
- Entities, DTOs, services and views organized by resource type
- See [Project Instructions](AGENTS.md) for coding standards and architecture
