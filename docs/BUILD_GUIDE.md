# Build Guide

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website:
<https://quarkus.io/>.

## ⚠️ Requirements

| Name   | Build | Run | Download                                                                      |
| ------ | ----- | --- | ----------------------------------------------------------------------------- |
| JDK 25 | ✅    | ❌  | [Adoptium](https://adoptium.net/temurin/releases/?os=any&arch=any&version=25) |
| Docker | ❌    | ✅  | (see below)                                                                   |

Every Java `make` target needs python3 ≥ 3.11. The first target that runs Maven
downloads devkit's pinned Maven, so it needs curl, tar and sha256sum/shasum (no
system Maven is needed; `./mvnw` stays for IDE or bare use). `make lint` and
`make format` need node ≥ 22.22.2 with npm (the Markdown gate). Fetching a
devkit release that isn't cached needs `ssh-keygen` (OpenSSH 8.1 or later),
which in a container needs the running UID to have a passwd entry.

For Docker, you have 2 options:

- [Docker Engine](https://docs.docker.com/engine/install/)
- or [Docker Desktop](https://docs.docker.com/desktop/) (includes Docker Engine)

## 🔧 Setup

### 1. Set Properties for AI API Keys

For development, set the following properties (only needed if using cloud AI
providers):

```sh
export APP_GOOGLE_API_KEY=your_google_api_key_here
export APP_OPENAI_API_KEY=your_openai_api_key_here
```

Alternatively, create a `.env` file in the project root and source it:

```sh
# .env
APP_GOOGLE_API_KEY=your_google_api_key_here
APP_OPENAI_API_KEY=your_openai_api_key_here

# source it
source .env
```

> **_NOTE:_** API keys are immutable configuration sourced from environment
> variables. All other AI settings (model, temperature, prompts, optional OpenAI
> Organization ID, etc.) are
> configured at runtime via the Admin Settings UI (`/admin/config`) after
> logging in.

### 1a. Encryption Key Setup

AIMathTutor encrypts PII fields (email) at rest using AES-256-GCM. The master
key is stored in a file on disk.

**Development (default):** No action required. On first startup the application
auto-generates a 256-bit key at `~/.local/share/aimathtutor/encryption.key` (or
`$XDG_DATA_HOME/aimathtutor/encryption.key` if set). Permissions are set to 600
(owner read/write only).

**Production (Docker Compose):** [deploy/compose.yml](../deploy/compose.yml),
downloaded as described in [Quickstart](QUICKSTART.md#using-docker-compose),
already configures the
`aimathtutor_keys` named volume and the `app.security.encryption-key-file`
property. No extra steps needed.

**Production (custom path):** Set `app.security.encryption-key-file` to an
absolute path writable by the application process. The file must contain a
Base64-encoded 32-byte key. The application generates it if absent.

> **⚠️ Critical:** Back up the key file (or the `aimathtutor_keys` Docker
> volume). Losing the key renders all encrypted data permanently unrecoverable.
> Key resolution order:
>
> 1. `app.security.encryption-key-file` property (if set and non-empty)
> 2. `$XDG_DATA_HOME/aimathtutor/encryption.key` (if file exists)
> 3. `~/.aimathtutor/encryption.key` (if file exists)
> 4. Auto-generate at the XDG path

### 1b. Setting Up Ollama (Optional)

If you want to use Ollama as your AI provider for local, privacy-focused LLM
inference, you have two options:

#### Option 1: Docker Compose (Recommended)

Use the production [deploy/compose.yml](../deploy/compose.yml), downloaded as
`docker-compose.yml` beside `.env` following the
[Quickstart](QUICKSTART.md#using-docker-compose). Uncomment its entire `ollama:`
service block under `services:` and `ollama_data:` under the top-level
`volumes:`. Keep the existing encryption key, log and database volumes.
The [Ollama setup](QUICKSTART.md#setup-with-docker-compose-recommended) covers
CPU, NVIDIA and AMD configuration. Run these commands from that download
directory:

```sh
docker compose up -d
docker compose exec ollama ollama pull qwen3:8b
```

Configure `http://ollama:11434` as the Ollama API URL in Admin Settings.
For development from a checkout, the root `docker-compose.yml` also includes
an optional `ollama` service and `ollama_data` volume.

#### Option 2: Host Installation (Development)

Download and install Ollama from
[ollama.com/download](https://ollama.com/download):

**Linux:**

```sh
curl -fsSL https://ollama.com/install.sh | sh
```

**macOS/Windows:**
Download the installer from the website.

#### Pull a Model

Ollama requires you to download models before use. **Important:** The Ollama
desktop app may not show all available models in its GUI - you need to pull
models using the command line even if they don't appear in the app.

Open a terminal/command prompt and run one of the following:

```sh
# Top Recommendations for Math Tutoring (2025):
ollama pull qwen3:0.6b                  # Ultra-light, 0.5GB, best for low-resource systems
ollama pull qwen3:1.7b                  # Compact model, 1.3GB, good balance for basic math
ollama pull qwen3:4b                    # Recommended balanced option, 2.5GB, rivals 7B performance
ollama pull qwen3:8b                    # High-quality math tutoring, 5.4GB, 40K context
ollama pull llama3.2:1b                 # Meta's tiny model, 1.3GB, fast and efficient
ollama pull llama3.2:3b                 # Meta's small model, 2GB, good reasoning
ollama pull llama3.1:8b                 # Meta's flagship 8B, 4.7GB, excellent general AI
ollama pull phi4-mini:3.8b              # Microsoft's efficient model, 2.2GB, strong reasoning
ollama pull phi4-mini-reasoning:3.8b    # Enhanced reasoning variant, 2.2GB, step-by-step math
ollama pull phi4:14b                    # Full-size model, 8.8GB, top-tier performance
ollama pull phi4-reasoning:14b          # Reasoning-optimized 14B, 8.8GB, best for complex problems
ollama pull deepseek-r1:1.5b            # Tiny reasoning model, 1.1GB, ultra-fast
ollama pull deepseek-r1:7b              # Strong reasoning at 7B, 4.7GB, excellent for math
ollama pull deepseek-r1:8b              # Enhanced reasoning model, 5.1GB, superior math ability
ollama pull gemma3:270m                 # Google's smallest, 0.2GB, experimental but fast
ollama pull gemma3:1b                   # Google's compact model, 0.7GB, efficient baseline
ollama pull gemma3:4b                   # Google's mid-size, 2.7GB, solid math performance
```

After pulling, the models will appear in your Ollama app and be available for
use.

#### Verify Ollama is Running

```sh
curl http://localhost:11434/api/tags
```

This should return a JSON list of installed models.

#### Configure AIMathTutor to Use Ollama

After starting the application, log in with admin credentials and navigate to
**Admin Settings** (`/admin/config`):

1. Set **AI Provider** to `ollama`
2. Set **Ollama API URL**:
   - **Docker Compose:** `http://ollama:11434`
   - **Host Installation (dev mode):** `http://localhost:11434`
   - **Host Installation (Docker, accessing host):**
     `http://host.docker.internal:11434`
3. Set **Ollama Model** to the model you pulled (e.g., `qwen3:8b`,
   `deepseek-r1:8b`, `llama3.1:8b`)
4. Adjust temperature (0.0-2.0, default 0.7) and max tokens as needed

> **_NOTE:_** Unlike cloud providers, Ollama runs locally and doesn't require
> API keys. All processing happens on your machine, ensuring data privacy.

#### GPU vs CPU Performance

- **CPU Mode (Default):** Works on any system, slower inference (~5-30 seconds
  per response depending on model size)
- **NVIDIA GPU Mode:** Requires NVIDIA GPU + Container Toolkit, significantly
  faster (~1-5 seconds per response)
- **AMD GPU Mode (ROCm):** Requires AMD GPU (RX 6000/7000 series or newer) with
  ROCm drivers, similar performance to NVIDIA
- **Recommendation:** Start with CPU mode and smaller models (`qwen3:4b`,
  `llama3.2:3b`) for testing. If performance is critical and you have a
  compatible GPU, enable GPU support.

To check GPU availability in the Ollama container:

```sh
# NVIDIA:
docker compose exec ollama nvidia-smi

# AMD:
docker compose exec ollama rocm-smi
```

### 2. Install Dependencies and Build

```sh
make install   # Installs dependencies, skips tests
```

### 🧪 Tests

Run unit tests (skips integration tests):

```sh
make test
```

Run all tests (unit + integration) and generate a coverage report:

```sh
make coverage
```

## 🚀 Running the application

### 🧑‍💻 Development mode

```sh
make dev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode
> only at <http://localhost:9001/q/dev/>.

### 🏭 Production build on the development stack

This runs the production build locally with the repository's development
compose file, which builds the app from the checkout. For a real deployment, use
[deploy/compose.yml](../deploy/compose.yml), which runs the published image;
follow the [Quickstart](QUICKSTART.md#using-docker-compose) to download it.

First, package the application for production:

```sh
make build    # Build the JVM Docker images for the local image store (runs Maven package)
```

Then run with Docker Compose:

```sh
docker compose up -d --build
```

## 📦 Packaging the application

You can package the application using:

```sh
make build
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the
`target/quarkus-app/lib/` directory.

The application is now runnable using
`java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar -Pproduction
```

The application, packaged as an _über-jar_, is now runnable using
`java -jar target/*-runner.jar`.

### 🐳 Creating a Docker image

You can create a Docker image using:

```sh
make build    # Build the JVM Docker images for the local image store
make release  # Test, tag, and build and push multi-platform images
```

`make build` builds `gregordietrich/aimathtutor:<version>-alpine` (also tagged
`<version>`) and `<version>-ubuntu` for the host's own platform and loads them
into the local image store.

`make release` asks for a version (it refuses up front anything `make tag`
would: only `X.Y.Z`, optionally prefixed `v` and followed by `rcN` or `-rcN`,
passes, so no `-SNAPSHOT`) and pulls `main`. Then it checks that the current
buildx builder can build both `linux/amd64` and `linux/arm64` (the default
`docker` driver needs the containerd image store for that; a `docker-container`
builder works on either store) and logs in to the registry. It cleans,
installs, lints, tests and packages the application, and builds both images for
both platforms into the buildx cache only. Then it runs `make tag`. Last, one
`docker buildx build --platform linux/amd64,linux/arm64 --push` per Dockerfile
publishes all of its tags together from the cached build: `<version>-alpine`,
`alpine`, `<version>` and `latest` for Alpine,
`<version>-ubuntu` and `ubuntu` for Ubuntu.

A release pushes nothing from the local image store. Any failure stops it with
an error. Up to the cache-only builds, nothing has been tagged or pushed. After
the git tag only a push can fail: an image pushed before the failure stays
published, and the git tag stays in place (`make untag` removes it).

Both Dockerfiles run Java directly as PID 1, with required runtime arguments
in their exec-form entrypoints. Add operator options with `JDK_JAVA_OPTIONS`,
for example `docker run -e JDK_JAVA_OPTIONS="-Xmx512m" ...`, or set
`JDK_JAVA_OPTIONS=-Xmx512m` in the deployment `.env` used by Compose. Java
prints the value to the container log, so keep secrets out of it.

If you want to learn more about building Docker images, please consult
<https://quarkus.io/guides/container-image>.

## ⚠️ Known Build Warnings

### Java compilation: warnings fail the build

Java compilation runs with a fail-on-warning policy: any javac lint or Error
Prone warning aborts the build (a small allowlist of non-actionable lint
categories is documented in devkit's parent POM, `.devkit/java/parent/pom.xml`).
If your build fails with `warnings found and -Werror specified`, fix the
reported warning — do not suppress it or exclude the lint category. See the Code
Quality Gates section in [AGENTS.md](../AGENTS.md) for details.

### Quarkus build: intentional observer warning

Every build logs `[WARNING]
[io.quarkus.arc.deployment.ObserverValidationProcessor] The method
de.vptr.aimathtutor.ProductionProfileGuard#checkProfiles is an observer for
@Initialized(ApplicationScoped.class) ... We strongly recommend to observe
StartupEvent instead`. The same warning is logged for
`SchemaManagementGuard#checkStrategy`. This is deliberate: both guards have to
run before Hibernate's schema management, which `StartupEvent` does not. Do not
"fix" it.

### Frontend build: expected non-fatal warnings

The frontend toolchain (`prepare-frontend`/`build-frontend`, run with
`-Pproduction`) is **not** covered by the fail-on-warning policy. Warnings you
may see there are expected and harmless:

- On a fresh checkout, **npm may print deprecation or peer-dependency notices**
  for bundled `@vaadin/*` packages while installing `node_modules`.

These warnings originate in Vaadin's published packages, cannot be fixed in this
repository, and do not fail the build. They will disappear with future Vaadin
upgrades — do not try to suppress or "fix" them locally.

## 📖 Related Guides & Docs

- [Quickstart](QUICKSTART.md)
- [Project Instructions](../AGENTS.md)

- Quarkus ([guide](https://quarkus.io/guides/)): The main framework for building
  Java applications with a focus on cloud-native and microservices
  architectures.
- Vaadin Flow
  ([guide](https://vaadin.com/docs/latest/flow/integrations/quarkus)): Vaadin
  Flow is a unique framework that lets you build web apps without writing HTML
  or JavaScript
- ArC ([guide](https://quarkus.io/guides/cdi-reference)): A dependency injection
  framework that is part of Quarkus, providing support for CDI (Contexts and
  Dependency Injection).
- Datasource ([guide](https://quarkus.io/guides/datasource)): A Quarkus
  extension for connecting to databases using JDBC, JPA, Hibernate ORM, and
  more.
- Hibernate ORM with Panache
  ([guide](https://quarkus.io/guides/hibernate-orm-panache)): A Quarkus
  extension that simplifies the use of Hibernate ORM with a focus on ease of use
  and productivity.
- Hibernate Validator ([guide](https://quarkus.io/guides/hibernate-validator)):
  A Quarkus extension that integrates Hibernate Validator for bean validation,
  allowing you to validate your data models easily.
