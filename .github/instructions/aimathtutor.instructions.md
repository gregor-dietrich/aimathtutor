---
applyTo: '**'
---
# AIMathTutor Project Coding Instructions

## Project Structure
- Full Stack web application using Quarkus + Vaadin
- Always cross-reference corresponding files between frontend and backend
- Reference existing code in same package for consistency and avoid duplication

## Framework Guidelines
- Follow Quarkus best practices
- Utilize Vaadin components for UI
- Check the `pom.xml` file for available dependencies and utilities

## Code Organization
- For each resource, there should be corresponding classes:
  - DTO classes (data transfer)
  - Entity (database access)
  - Service (business logic)
  - View (UI component)

## Testing Standards
- Reference existing test structure and practices
- Be careful with class references - service names may overlap between projects but have different packages
- Run tests after creation and fix compilation/test failures

## Development Workflow
1. Make changes following project coding standards
2. Add clear comments where necessary
3. Run `./mvnw clean install package -DskipTests && ./mvnw test` to rebuild and test
4. Fix any compilation errors or test failures
5. If issues persist, run `./mvnw quarkus:dev` (in either or both sub-projects as necessary) and check logs

## Code Quality
- Write clear, concise code
- Ensure changes don't break existing functionality
- Follow established patterns within the codebase
