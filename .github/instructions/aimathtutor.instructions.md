---
applyTo: '**'
---
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
