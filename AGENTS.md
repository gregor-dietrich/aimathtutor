---
applyTo: '**'
---

# AIMathTutor Project Coding & Architecture Guide

## Overview

AIMathTutor is a monolithic full-stack web application built with **Quarkus** (backend) and **Vaadin** (frontend). It provides interactive math exercises, AI-powered tutoring, and analytics for students and teachers. The project tightly integrates backend and frontend using CDI (`@Inject`), with no REST API boundary between core services and views.

## Key Features

- **Graspable Math Workspace:** Embedded for symbolic manipulation and step-by-step math actions.
- **AI Tutor Layer:** Real-time feedback, hints, and adaptive problem generation using multiple AI providers (Gemini, OpenAI, Ollama, mock).
- **Lesson & Exercise Management:** Author, organize, and track progress on math problems and lessons.
- **Comment System:** Threaded comments for exercises, moderation, and reporting.
- **User Management:** Roles (Admin, Teacher, Student), groups, ranks, and permissions.
- **Analytics:** Session/event tracking, progress summaries, and teacher/admin dashboards.

## Project Structure

- **Backend & Frontend:**
  - Vaadin views directly inject backend services (CDI, `@Inject`).
  - No REST API for core app logic; REST only for external AI APIs.
- **Graspable Math Integration:**
  - Embedded via Vaadin `Html`/`IFrame` components and JavaScript API.
  - Student actions captured by JS listeners, sent to Java via `@ClientCallable`.
- **AI Tutor Integration:**
  - `AITutorService` orchestrates feedback, hints, and problem generation.
  - Supports Gemini, OpenAI, Ollama, and mock providers (configurable via `application.properties`).
  - AI feedback is returned as `AIFeedbackDto` and displayed in chat-style panels.
- **Entities, DTOs, Services, Views:**
  - For each resource, maintain:
    - **DTOs:** Data transfer objects (e.g., `GraspableEventDto`, `AIFeedbackDto`, `CommentDto`).
    - **Entities:** Hibernate/Panache entities for DB access (e.g., `StudentSessionEntity`, `AIInteractionEntity`, `CommentEntity`).
    - **Services:** Business logic (`@ApplicationScoped`), e.g., `AITutorService`, `GraspableMathService`, `CommentService`, `AnalyticsService`.
    - **Views:** Vaadin UI components (e.g., `MathWorkspaceView`, `ExerciseWorkspaceView`, `LessonsView`).

## AI Providers

- **Gemini (Google):**
  - Configure via `gemini.api.key`, `gemini.model`, etc.
  - See `GeminiAIService.java` and DTOs for integration.
- **OpenAI:**
  - Configure via `openai.api.key`, `openai.model`, etc.
  - See `OpenAIService.java` and DTOs for integration.
- **Ollama (local LLM):**
  - Configure via `ollama.api.url`, `ollama.model`, etc.
  - See `OllamaService.java` and DTOs for integration.
- **Mock Provider:**
  - For development/testing, set `ai.tutor.provider=mock` or disable with `ai.tutor.enabled=false`.

## Graspable Math + AI Integration Logic

1. **Student Action:** Performed in Graspable Math workspace (move, simplify, expand, etc.).
2. **Event Capture:** JavaScript listener calls Java method via `@ClientCallable`.
3. **DTO Conversion:** Event data mapped to `GraspableEventDto`.
4. **AI Analysis:** View calls `AITutorService.analyzeMathAction(eventDto)`.
5. **AI Feedback:** Service constructs prompt, queries AI, returns `AIFeedbackDto`.
6. **UI Update:** Feedback displayed in chat panel (`AIChatPanel`) beside workspace.
7. **Session/Event Logging:** Actions and feedback logged for analytics.

## User Management & Permissions

- **Entities:** `UserEntity`, `UserGroupEntity`, `UserRankEntity`.
- **Roles:** Admin, Teacher, Student (see `user_ranks` table in `init.sql`).
- **Groups & Ranks:** Used for differentiated access, progress tracking, and permissions.
- **Authentication:** Managed by `AuthService`, with password hashing and session management.

## Comments & Moderation

- **CommentEntity:** Stores threaded comments on exercises.
- **CommentService:** Handles creation, editing, deletion, flagging, and moderation.
- **CommentsPanel:** Vaadin component for displaying and managing comments.
- **Moderation:** Rate limiting, flagging, auto-hide, and admin/teacher controls.

## Analytics & Progress Tracking

- **StudentSessionEntity:** Tracks sessions per user/exercise.
- **AIInteractionEntity:** Logs AI feedback and interactions.
- **AnalyticsService:** Provides summaries, progress reports, and admin dashboards.

## Testing Standards

- **Unit & Integration Tests:**
  - Mock AI endpoints for deterministic tests.
  - Test Graspable Math event handling and feedback logic.
  - Use test DTOs/entities mirroring main code structure.
- **Run tests after changes:**
  - `./mvnw clean install package -DskipTests && ./mvnw test`

## Development Workflow

1. Make changes following these instructions and project coding standards.
2. Add clear comments and Javadoc where necessary.
3. Reference existing code for consistency and avoid duplication.
4. Run tests and fix compilation/test failures before committing.
5. Use seeded test accounts (see `init.sql`) for local testing.

## Configuration & Environment

- **Main config:** `src/main/resources/application.properties`
- **Database:** PostgreSQL (default), see `init.sql` for schema and seed data.
- **Logging:** Configured for file and console output; see comments in `application.properties`.
- **Docker:** Use `docker-compose.yml` for production deployment.

## Documentation

- [Quickstart](docs/QUICKSTART.md)
- [Build Guide](docs/BUILD_GUIDE.md)
- [README.md](README.md)

## Changelog Standards

**Changelog Directory:** All changelogs are maintained in the `changelog/` directory, with a separate `.md` file for each released version (e.g., `changelog/1.0.0.md`, `changelog/1.1.0.md`).

**Format:** Each changelog file follows [Semantic Versioning 2.0.0](https://www.semver.org) and [Keep a Changelog 1.1.0](https://www.keepachangelog.com).

**Content:**

- Each entry describes changes from an end user's perspective (features, fixes, removals, breaking changes, etc.).
- Do **not** mention class, method, or file names—focus on what changed for users, not implementation details.
- Document all changes introduced by commits since the last tagged release in a new version file.
- Use clear sections: Added, Changed, Deprecated, Removed, Fixed, Security, and **Unreleased**.
- The **Unreleased** section lists upcoming changes that are not yet released, so users can see what to expect. At release time, move these changes into the new version section.
- Update the changelog with every meaningful change before merging or tagging a release.
- For initial releases, provide a brief explanation of the changelog and versioning approach.
