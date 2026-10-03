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
- AI providers: `app.google.api.key`, `app.openai.api.key`, `app.openai.organization-id`
- pgAdmin: `PGADMIN_EMAIL`, `PGADMIN_PASSWORD`
- **Encryption key**: `app.security.encryption-key-file=/etc/aimathtutor/keys/encryption.key` — must be set in the app service environment. Mount the `aimathtutor_keys` named volume at `/etc/aimathtutor/keys`. **Back up this volume** — losing it makes all encrypted PII permanently unrecoverable.
- **Never hardcode real secrets** as defaults. Use placeholders (e.g., `changeit`).

## Build Script

`scripts/build.sh` (invoked via `make build`):

1. Runs `scripts/check.sh` (JDK + Maven version verification)
2. Runs `mvn clean`, removes `src/main/bundles/prod.bundle` and `node_modules`, then
   `mvn package -DskipTests -Pproduction`
3. Builds both images for the host's platform into the local image store (`docker buildx build --load`, or plain
   `docker build` without buildx): `<revision>-alpine` (also tagged `<revision>`) and `<revision>-ubuntu`

## Release Script

`scripts/release.sh` (invoked via `make release`):

1. Refuses a `-SNAPSHOT` version, pulls `main` and reruns itself from the pulled scripts
2. Fails unless the current buildx builder lists `linux/amd64` and `linux/arm64` (the `docker` driver also needs the
   containerd image store), then runs `docker login`
3. Cleans, installs, lints, tests and packages, then builds each Dockerfile for both platforms into the buildx cache
   only, so only a push can fail after the git tag
4. Runs `scripts/tag.sh`, then one `docker buildx build --platform linux/amd64,linux/arm64 --push` per Dockerfile
   with all of its tags: Alpine `<version>-alpine`, `alpine`, `<version>`, `latest`; Ubuntu `<version>-ubuntu`,
   `ubuntu`. Nothing is pushed from the local image store

Shared helpers live in `scripts/lib/images.sh`.

## Logging

- Console: plain text in dev/test, JSON in production
- File: enabled in production at `logs/aimathtutor.log`, rotated (10MB max, 5 backups)
- Dev/test: file logging disabled (`quarkus.log.file.enabled=false`)
- Log format: `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n`

## Production Notes

- JVM args required: `--add-opens java.base/java.lang=ALL-UNNAMED`, `--add-opens java.base/jdk.internal.ref=ALL-UNNAMED`, `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED`, `--add-opens java.base/java.nio=ALL-UNNAMED`, `--add-opens java.base/sun.nio.ch=ALL-UNNAMED`, `--enable-native-access=ALL-UNNAMED`, `--sun-misc-unsafe-memory-access=allow`, and `-XX:+EnableDynamicAgentLoading` (set in `JAVA_OPTS_APPEND` in both Dockerfiles)
- Schema: Flyway creates and upgrades it on startup in every profile; Hibernate only validates (`validate`)
- AI runtime config: DB-backed, managed via Admin Settings UI at `/admin/config`
