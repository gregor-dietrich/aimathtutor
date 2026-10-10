---
description: "Use when modifying the Dockerfiles or docker-compose.yml. Covers keeping the two images in step and compose defaults. Builds, releases and the key volume are in AGENTS.md, Docker."
paths:
  - "src/main/docker/**"
  - "docker-compose.yml"
  - "docs/QUICKSTART.md"
---

# Docker Conventions

- **Two images, one layout:** `Dockerfile.alpine` and `Dockerfile.ubuntu`
  differ only where the distribution does: the base image, the package and
  user-creation commands, and the healthcheck tool (`wget` on Alpine, `curl`
  on Ubuntu). A change to one goes into the other:
  the Temurin 25 JRE base pinned by digest, the fast-jar copy to
  `/deployments`, user `185`, port `9001`, the `/q/health/ready`
  healthcheck, and the `JAVA_OPTS_APPEND` list, which must match the JVM
  args in `AGENTS.md`.
- **Compose defaults are placeholders:** every credential or
  deployment-specific value comes from `${VAR:-default}`, and a default is
  never a real secret (`changeit`, `not-configured`). The same holds for the
  production compose file in `docs/QUICKSTART.md`.
- **The encryption key** stays on the `aimathtutor_keys` volume at the
  `app.security.encryption-key-file` path; see `AGENTS.md`, "Docker".
