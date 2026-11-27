---
description: "Use when modifying the Dockerfiles, the Compose files or Dependabot's entry for the Dockerfiles. Covers keeping the two images in step, the base image's trusted roots, Dependabot's Temurin changes and compose defaults. Builds, releases and the key volume are in AGENTS.md, Docker."
paths:
  - "src/main/docker/**"
  - "docker-compose.yml"
  - "deploy/compose.yml"
  - "docs/QUICKSTART.md"
  - ".github/dependabot.yml"
---

# Docker Conventions

- **Two images, one layout:** `Dockerfile.alpine` and `Dockerfile.ubuntu`
  differ only where the distribution does: the base image, the package and
  user-creation commands, and the healthcheck tool (`wget` on Alpine, `curl`
  on Ubuntu). A change to one goes into the other:
  the Temurin 25 JRE base, pinned by digest in a single `FROM` at the same
  `<version>_<build>` in both (`TemurinBasePinTest`), the fast-jar copy to
  `/deployments`, user `185`, port `9001`, the `/q/health/ready`
  healthcheck, and the runtime JVM arguments in the exec-form Java
  entrypoint, each with its measured reason in `AGENTS.md` ("Runtime JVM
  arguments") and checked by `JvmArgsParityTest`; add or drop one only after
  measuring the image, and pass no `-D` settings there. Java stays PID 1; add
  operator options via `JDK_JAVA_OPTIONS`, without baking its value into the
  image.
- **Trusted roots come from the Temurin base:** the images reject the
  compromised eMudhra intermediates only because the base's `cacerts` lacks
  `emSign ECC Root CA - G3`, `emSign ECC Root CA - C3`, `emSign Root CA - C1`
  and `AAA Certificate Services`; its `blocked.certs` lists none of them. A
  Temurin bump keeps all four out: check its `cacerts`, or that the eMudhra
  test hosts at `emudhra.we-have-a-problem.com` still fail certificate path
  validation from the image while a control host such as `api.openai.com`
  succeeds.
- **Dependabot proposes Temurin changes:** its `/src/main/docker` entry
  matches the dependency by name (`eclipse-temurin`; a pattern never sees the
  tag) and ignores Java 26 and later.
  - A change arrives in the weekly `infrastructure` group PR, beside other
    ecosystems' updates: a version bump, or a new digest under the same tag,
    because Temurin pushes its version tags again.
  - Dependabot resolves each Dockerfile's tag on its own. While only one
    distribution has the new version, `TemurinBasePinTest` fails that PR;
    wait for the other tag, or set it by hand.
  - No CI step reads an image's `cacerts`: the trusted-roots check is by hand,
    for any change to either digest.
  - The Alpine tag carries no release number (`-jre-alpine`, not
    `-jre-alpine-3.24`). Dependabot reads a trailing release number as a build
    number and then proposes a floating major tag (`25-jre-alpine-3.23` from
    `25.0.5.1_2-jre-alpine-3.24`), so `TemurinBasePinTest` allows only letters
    after `-jre-`. A new Alpine release therefore shows only in the digest.
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
