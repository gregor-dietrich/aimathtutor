# Build Guide

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## ⚠️ Requirements

| Name | Build | Run | Download |
|----------|----------|----------|----------|
| JDK 25 | ✅ | ❌ | [Adoptium](https://adoptium.net/temurin/releases/?os=any&arch=any&version=25) |
| Maven | ✅ | ❌ | [Apache](https://maven.apache.org/download.cgi) |
| Docker | ❌ | ✅ | (see below) |

For Docker, you have 2 options:

- [Docker Engine](https://docs.docker.com/engine/install/)
- or [Docker Desktop](https://docs.docker.com/desktop/) (includes Docker Engine)

## 🔧 Setup

### 1. Set Environment Variables for AI API Keys

For development, set the following environment variables (only needed if using real AI providers):

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
