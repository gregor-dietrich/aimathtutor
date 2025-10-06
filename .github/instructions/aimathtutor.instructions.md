---
applyTo: '**'
---
# AIMathTutor Project Coding Instructions

## Project Structure
- **Monolithic** full-stack web application using **Quarkus + Vaadin**
- Backend and frontend are **tightly integrated** - no REST API between them
- Vaadin Views directly inject and call backend Services using CDI (`@Inject`)
- Integrates **Graspable Math** as an interactive math workspace
- Adds an **AI Tutor Layer** for feedback, hints, and adaptive problem generation
- Reference existing code in same package for consistency and avoid duplication

## Framework Guidelines
- Follow Quarkus best practices (CDI, ApplicationScoped services, etc.)
- Utilize Vaadin components for UI (Flow components, layouts, etc.)
- Services are injected into Views using `@Inject` annotation
- Use the Graspable Math **JavaScript API / embed widget** in Vaadin views
- Check the `pom.xml` file for available dependencies and utilities

## Code Organization
- For each resource, there should be corresponding classes:
  - **DTO classes:** for data transfer and view data (e.g., `GraspableEventDto`, `AIFeedbackDto`)
  - **Entity classes:** for database access with Hibernate/Panache (e.g., `StudentSessionEntity`, `AIInteractionEntity`)
  - **Service classes:** for business logic with `@ApplicationScoped` or `@Dependent` (e.g., `AITutorService`, `GraspableMathService`)
  - **View classes:** Vaadin UI components extending layouts (e.g., `GraspableMathView`, `AITutorView`)
- Add new resources for AI integration:
  - `AITutorService.java`: handles calls to AI APIs (e.g., OpenAI, local ML models) - `@ApplicationScoped`
  - `AIFeedbackDto.java`: defines structure of AI feedback responses
  - `AIInteractionEntity.java`: stores AI interactions in database (optional, for logging/analytics)
- Add new resources for Graspable Math workspace integration:
  - `GraspableEventDto.java`: represents student actions (move, simplify, expand, etc.)
  - `GraspableMathView.java`: Vaadin view embedding Graspable Math using `Html` or `IFrame` component
  - `GraspableMathService.java`: handles Graspable Math event processing and state management

## Graspable Math + AI Integration Logic
- **View Layer:** `GraspableMathView` embeds the Graspable Math workspace (HTML/JS widget)
- **JavaScript Integration:** Use Vaadin's `@JavaScript` annotation or `UI.getCurrent().getPage().executeJs()` to interact with Graspable Math
- **Event Flow:**
  1. Student performs action in Graspable Math (move, simplify, etc.)
  2. JavaScript listener captures event and calls Java method via `element.$server.methodName(eventData)`
  3. View method receives event, converts to `GraspableEventDto`
  4. View calls `AITutorService.analyzeMathAction(eventDto)` (direct method call, no REST)
  5. `AITutorService` constructs prompt and queries AI model
  6. Service returns `AIFeedbackDto` to view
  7. View displays feedback in Vaadin component (e.g., `Div`, `Paragraph`, chat-style layout)
- **AI Feedback Examples:**
  - "Good job, correct simplification!"
  - "Careful, you only divided one side of the equation."
  - "Try factoring instead of expanding."
- **AI Additional Features:**
  - Generate new problems based on student performance
  - Summarize learning progress
  - Provide teacher-facing reports of student strengths and weaknesses
# AIMathTutor Project Coding Instructions

## Project Structure
- Full Stack web application using **Quarkus + Vaadin**
- Integrates **Graspable Math** as an interactive math workspace
- Adds an **AI Tutor Layer** for feedback, hints, and adaptive problem generation
- Always cross-reference corresponding files between frontend and backend
- Reference existing code in same package for consistency and avoid duplication

## Framework Guidelines
- Follow Quarkus best practices
- Utilize Vaadin components for UI
- Use the Graspable Math **JavaScript API / embed widget** in frontend views
- Communicate between Graspable Math and AI services via REST endpoints
- Check the `pom.xml` file for available dependencies and utilities

## Code Organization
- For each resource, there should be corresponding classes:
  - **DTO classes:** for data exchange (e.g., student steps, AI feedback)
  - **Entity:** for database access (e.g., student sessions, logs)
  - **Service:** for business logic (e.g., AI analysis, feedback generation)
  - **View:** for UI components (e.g., Graspable Math canvas + AI chat window)
- Add a new resource for AI integration
  - `AITutorService.java`: handles calls to AI APIs (e.g., OpenAI, local ML models)
  - `AIResponseDTO.java`: defines structure of AI feedback
  - `AIController.java`: exposes REST endpoints for the frontend
- Add a new resource for Graspable Math workspace integration
  - `GraspableEventDTO.java`: represents student actions (move, simplify, etc.)
  - `GraspableView.java`: Vaadin view embedding Graspable Math

## Graspable Math + AI Integration Logic
- The **frontend** embeds the Graspable Math workspace (HTML/JS widget)
- Each student action triggers a **JavaScript listener** that sends an event to the backend
- The **backend** receives this event and passes it to `AITutorService`
- `AITutorService` constructs a natural language or structured prompt and sends it to the AI model
- The AI analyzes the action and returns feedback such as:
  - “Good job, correct simplification.”
  - “Careful, you only divided one side.”
  - “Try factoring instead of expanding.”
- The feedback is displayed beside the Graspable Math workspace (e.g., in a Vaadin chat panel)
- AI can also:
  - Generate new problems based on student performance
  - Summarize learning progress
  - Provide teacher-facing reports of student strengths and weaknesses

## Testing Standards
- Reference existing test structure and practices
- Add mock tests for AI endpoints (simulate API calls)
- Verify Graspable Math event handling using integration tests
- Run tests after creation and fix compilation/test failures
- Be careful with class references — service names may overlap between projects but have different packages

## Development Workflow
1. Make changes following project coding standards
2. Add clear comments where necessary
3. Run:
   ```bash
   ./mvnw clean install package -DskipTests && ./mvnw test
