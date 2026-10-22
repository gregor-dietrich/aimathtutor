---
description: "Use when modifying the Dockerfiles or Compose files. Covers keeping the two images in step and compose defaults. Builds, releases and the key volume are in AGENTS.md, Docker."
paths:
  - "src/main/docker/**"
  - "docker-compose.yml"
  - "deploy/compose.yml"
  - "docs/QUICKSTART.md"
---

# Docker Conventions

- **Two images, one layout:** `Dockerfile.alpine` and `Dockerfile.ubuntu`
  differ only where the distribution does: the base image, the package and
  user-creation commands, and the healthcheck tool (`wget` on Alpine, `curl`
  on Ubuntu). A change to one goes into the other:
  the Temurin 25 JRE base pinned by digest, the fast-jar copy to
  `/deployments`, user `185`, port `9001`, the `/q/health/ready`
  healthcheck, and the runtime JVM arguments in the exec-form Java
  entrypoint, each with its measured reason in `AGENTS.md` ("Runtime JVM
  arguments") and checked by `JvmArgsParityTest`; add or drop one only after
  measuring the image, and pass no `-D` settings there. Java stays PID 1; add
  operator options via `JDK_JAVA_OPTIONS`, without baking its value into the
  image.
- **Compose defaults are placeholders:** every credential or
  deployment-specific value comes from `${VAR:-default}`, or from
  `${VAR:?message}` where no placeholder should work, and a default is never a
  real secret (`changeit`, `not-configured`). The same holds for the
  production `deploy/compose.yml`, downloaded as `docker-compose.yml` by
  `docs/QUICKSTART.md`. Root Compose builds from the checkout; production
  Compose runs the published image. Keep PostgreSQL tag/digest parity with
  Dev Services as checked by `PostgresImagePinTest`.
- **The encryption key** stays on the `aimathtutor_keys` volume at the
  `app.security.encryption-key-file` path; see `AGENTS.md`, "Docker".
