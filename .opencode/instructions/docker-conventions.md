# Docker Conventions — AIMathTutor

## Dockerfiles

Located in `src/main/docker/`:

- **Dockerfile.alpine**: Alpine-based image. Smaller footprint.
- **Dockerfile.ubuntu**: Ubuntu-based image (Eclipse Temurin JRE). Primary image.

### Conventions

- Base image: Eclipse Temurin JRE (version matching project JDK 25)
- Application user: non-root
- Layer caching: Quarkus fast-jar layout (`quarkus-app/` directory structure)
- Logs: `mkdir -p /deployments/logs` — log file at `logs/aimathtutor.log`
- Health check: `wget --spider http://localhost:9001/q/health/ready`
- Exposed port: **9001** (configured via `quarkus.http.port`)
- Entrypoint: `java -jar /deployments/quarkus-run.jar`

### Build Prerequisite

Always package for production before building images:

```shell
./mvnw clean install package -DskipTests -Pproduction
```

The `-Pproduction` profile is **required** — it triggers Vaadin `prepare-frontend` + `build-frontend`.

## docker-compose.yml (project root)

Full-stack compose:

- **app** (AIMathTutor): port 9001, depends on `postgres` healthy
- **postgres**: port 55432→5432 (DevServices default)
- **pgadmin** (optional): port 42069→80
- **ollama** (optional): for local Ollama AI provider

## Environment Variables

- Substitution: `${VAR_NAME:-default}`
- Timezone: `TZ` (default `UTC`)
- Image tag: `REVISION` (default `1.0.0-SNAPSHOT`)
- Database: `SQL_USERNAME`, `SQL_PASSWORD`, `SQL_DATABASE`, `SQL_PORT`
- AI providers: `GOOGLE_API_KEY`, `OPENAI_API_KEY`, `OPENAI_ORG_ID`
- pgAdmin: `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`
- **Encryption key**: `AIMATHTUTOR_ENCRYPTION_KEY_FILE=/etc/aimathtutor/keys/encryption.key` — must be set in the app service. Mount the `aimathtutor_keys` named volume at `/etc/aimathtutor/keys`. **Back up this volume** — losing it makes all encrypted PII permanently unrecoverable.
- **Never hardcode real secrets** as defaults. Use placeholders (e.g., `changeit`).

## Build Script

`scripts/build.sh` (invoked via `make build`):

1. Runs `make check` (JDK + Maven version verification)
2. Runs `./mvnw clean install package -DskipTests -Pproduction`
3. Runs `docker buildx` with multi-platform support (QEMU fallback)
4. Tags image with project revision

## Logging

- Console: plain text in dev/test, JSON in production
- File: enabled in production at `logs/aimathtutor.log`, rotated (10MB max, 5 backups)
- Dev/test: file logging disabled (`quarkus.log.file.enabled=false`)
- Log format: `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n`

## Production Notes

- JVM args required: `--add-opens java.base/java.lang=ALL-UNNAMED`, `--add-opens java.base/jdk.internal.ref=ALL-UNNAMED`, `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED`, `--add-opens java.base/java.nio=ALL-UNNAMED`, `--add-opens java.base/sun.nio.ch=ALL-UNNAMED`, `--enable-native-access=ALL-UNNAMED`, `--sun-misc-unsafe-memory-access=allow`, and `-XX:+EnableDynamicAgentLoading` (set in `JAVA_OPTS_APPEND` in both Dockerfiles)
- Schema strategy: `validate` (production) — schema must already exist
- AI runtime config: DB-backed, managed via Admin Settings UI at `/admin/config`
