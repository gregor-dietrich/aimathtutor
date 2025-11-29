# Build Guide

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## ⚠️ Requirements

| Name | Build | Run | Download |
|----------|----------|----------|----------|
| JDK 21 | ✅ | ❌ | [Adoptium](https://adoptium.net/temurin/releases/?os=any&arch=any&version=21) |
| Maven | ✅ | ❌ | [Apache](https://maven.apache.org/download.cgi) |
| Docker | ❌ | ✅ | (see below) |

For Docker, you have 2 options:

- [Docker Engine](https://docs.docker.com/engine/install/)
- or [Docker Desktop](https://docs.docker.com/desktop/) (includes Docker Engine)

## 🔧 Setup

### 1. Set Environment Variables for AI API Keys

For development, set the following environment variables (only needed if using cloud AI providers):

```sh
export GEMINI_API_KEY=your_gemini_api_key_here
export OPENAI_API_KEY=your_openai_api_key_here
export OPENAI_ORG_ID=your_openai_org_id_here  # Optional
```

Alternatively, create a `.env` file in the project root and source it:

```sh
# .env
GEMINI_API_KEY=your_gemini_api_key_here
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_ORG_ID=your_openai_org_id_here

# source it
source .env
```

> **_NOTE:_** API keys are immutable configuration sourced from environment variables. All other AI settings (model, temperature, prompts, etc.) are configured at runtime via the Admin Settings UI (`/admin/config`) after logging in.

### 1a. Setting Up Ollama (Optional)

If you want to use Ollama as your AI provider for local, privacy-focused LLM inference, you have two options:

#### Option 1: Docker Compose (Recommended for Production)

The project includes an Ollama service in `docker-compose.yml` that you can enable:

1. **Uncomment the Ollama service** in `docker-compose.yml`:

   ```yml
   ollama:
     # Choose image based on your GPU:
     image: ollama/ollama:0.13.0        # CPU or NVIDIA GPU
     # image: ollama/ollama:0.13.0-rocm  # AMD GPU (ROCm)
     restart: unless-stopped
     volumes:
       - ollama_data:/root/.ollama
     # For NVIDIA GPU support, uncomment below:
     # deploy:
     #   resources:
     #     reservations:
     #       devices:
     #         - driver: nvidia
     #           count: all
     #           capabilities: [gpu]
     # For AMD GPU support:
     # 1. Use image: ollama/ollama:0.13.0-rocm above
     # 2. Uncomment below:
     # devices:
     #   - /dev/kfd
     #   - /dev/dri
     # group_add:
     #   - video
     healthcheck:
       test: ["CMD", "ollama", "ls"]
       interval: 10s
       timeout: 3s
       retries: 3
       start_period: 10s
   ```

2. **Uncomment the volume** in the `volumes:` section:

   ```yml
   volumes:
     aimathtutor_logs:
     ollama_data:  # Uncomment this line
     pgadmin_data:
     postgres_data:
   ```

3. **Start the stack:**

   ```sh
   docker compose up -d
   ```

4. **Pull a model** into the Ollama container:

   ```sh
   docker compose exec ollama ollama pull qwen3:8b
   # Or: docker compose exec ollama ollama pull qwen3:4b
   ```

5. **Configure AIMathTutor** to use `http://ollama:11434` as the Ollama API URL in Admin Settings.

> **GPU Support:** By default, Ollama runs on CPU. For GPU acceleration:
>
> - **NVIDIA:** Uncomment the `deploy` section. Requires NVIDIA GPU, drivers, and [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)
> - **AMD:** Use `ollama/ollama:0.13.0-rocm` image and uncomment AMD device mappings. Requires AMD GPU with ROCm support (RX 6000/7000 series or newer)

#### Option 2: Host Installation (Development)

Download and install Ollama from [ollama.com/download](https://ollama.com/download):

**Linux:**

```sh
curl -fsSL https://ollama.com/install.sh | sh
```

**macOS/Windows:**
Download the installer from the website.

#### Pull a Model

Ollama requires you to download models before use. **Important:** The Ollama desktop app may not show all available models in its GUI - you need to pull models using the command line even if they don't appear in the app.

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

After pulling, the models will appear in your Ollama app and be available for use.

#### Verify Ollama is Running

```sh
curl http://localhost:11434/api/tags
```

This should return a JSON list of installed models.

#### Configure AIMathTutor to Use Ollama

After starting the application, log in with admin credentials and navigate to **Admin Settings** (`/admin/config`):

1. Set **AI Provider** to `ollama`
2. Set **Ollama API URL**:
   - **Docker Compose:** `http://ollama:11434`
   - **Host Installation (dev mode):** `http://localhost:11434`
   - **Host Installation (Docker, accessing host):** `http://host.docker.internal:11434`
3. Set **Ollama Model** to the model you pulled (e.g., `qwen3:8b`, `deepseek-r1:8b`, `llama3.1:8b`)
4. Adjust temperature (0.0-2.0, default 0.7) and max tokens as needed

> **_NOTE:_** Unlike cloud providers, Ollama runs locally and doesn't require API keys. All processing happens on your machine, ensuring data privacy.

#### GPU vs CPU Performance

- **CPU Mode (Default):** Works on any system, slower inference (~5-30 seconds per response depending on model size)
- **NVIDIA GPU Mode:** Requires NVIDIA GPU + Container Toolkit, significantly faster (~1-5 seconds per response)
- **AMD GPU Mode (ROCm):** Requires AMD GPU (RX 6000/7000 series or newer) with ROCm drivers, similar performance to NVIDIA
- **Recommendation:** Start with CPU mode and smaller models (`qwen3:4b`, `llama3.2:3b`) for testing. If performance is critical and you have a compatible GPU, enable GPU support.

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

Run the full test suite:

```sh
make test
```

## 🚀 Running the application

### 🧑‍💻 Development mode

```sh
make dev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:9001/q/dev/>.

### 🏭 Production mode

First, package the application for production:

```sh
make build    # Build the JVM Docker image (runs tests/install and Maven package)
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
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar -Pproduction
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

### 🐳 Creating a Docker image

You can create a Docker image using:

```sh
make build    # Build the JVM Docker image
make release  # Build and push Docker image tag to registry
```

If you want to learn more about building Docker images, please consult <https://quarkus.io/guides/container-image>.

## 📖 Related Guides & Docs

- [Quickstart](QUICKSTART.md)
- [Project Instructions](../.github/instructions/aimathtutor.instructions.md)

- Quarkus ([guide](https://quarkus.io/guides/)): The main framework for building Java applications with a focus on cloud-native and microservices architectures.
- Vaadin Flow ([guide](https://vaadin.com/docs/latest/flow/integrations/quarkus)): Vaadin Flow is a unique framework that lets you build web apps without writing HTML or JavaScript
- ArC ([guide](https://quarkus.io/guides/cdi-reference)): A dependency injection framework that is part of Quarkus, providing support for CDI (Contexts and Dependency Injection).
- Datasource ([guide](https://quarkus.io/guides/datasource)): A Quarkus extension for connecting to databases using JDBC, JPA, Hibernate ORM, and more.
- Hibernate ORM with Panache ([guide](https://quarkus.io/guides/hibernate-orm-panache)): A Quarkus extension that simplifies the use of Hibernate ORM with a focus on ease of use and productivity.
- Hibernate Validator ([guide](https://quarkus.io/guides/hibernate-validator)): A Quarkus extension that integrates Hibernate Validator for bean validation, allowing you to validate your data models easily.
