
# AIMathTutor

AIMathTutor is a full-stack web application for interactive math learning, built with Quarkus (backend) and Vaadin (frontend). It features an embedded Graspable Math workspace, AI-powered tutoring, lesson/exercise management, analytics, and granular user roles.

## 🌟 Features

- Interactive Graspable Math workspace for symbolic manipulation and step-by-step actions
- Real-time AI tutor feedback, hints, and adaptive problem generation (Gemini, OpenAI, Ollama, mock)
- Problem and lesson authoring, organization, and progress tracking
- Threaded comments on exercises, moderation, and reporting
- Session/event tracking and analytics dashboards for teachers/admins
- Granular user management: users, groups, ranks, and permissions
- Tight Quarkus + Vaadin integration: CDI-injected services, no REST boundary for core logic

## 🚀 Getting Started

See [Quickstart](docs/QUICKSTART.md) for setup and usage.

### Common Development Commands (via Makefile)

- `make dev`      – Start Quarkus in dev mode
- `make test`     – Execute the Maven test suite
- `make build`    – Build the Docker image (`make check`, `mvn package`, `docker buildx`)
- `make install`  – `make check` and `mvn clean install -DskipTests`
- `make password` – Generate a salt+hash for a password (for init.sql)
- `make release`  – Pull from origin/main, `make build`, `make tag`, and push Docker image tag to registry
- `make branch`, `make tag`, `make rebase`, `make untag` – Git branch/tag management

See the [Makefile](Makefile) or use `make help` for all available commands and scripts.

## 🤖 Supported AI Providers

- [Google Gemini](https://aistudio.google.com/api-keys)
- [Ollama](https://ollama.com/download)
- [OpenAI](https://platform.openai.com/api-keys)

Configure providers in `src/main/resources/application.properties` or via environment variables. See [docs/QUICKSTART.md](docs/QUICKSTART.md) for details.

## 📖 Documentation

- [Quickstart](docs/QUICKSTART.md)
- [Build Guide](docs/BUILD_GUIDE.md)
- [Project Instructions](.github/instructions/aimathtutor.instructions.md)

## 🛠️ Project Structure & Workflow

- Monolithic Quarkus + Vaadin app
- Vaadin views inject backend services via CDI (`@Inject`)
- Graspable Math workspace embedded via Vaadin and JavaScript API
- AI Tutor layer supports Gemini, OpenAI, Ollama, and mock providers
- Entities, DTOs, services and views organized by resource type
- See [Project Instructions](.github/instructions/aimathtutor.instructions.md) for coding standards and architecture
