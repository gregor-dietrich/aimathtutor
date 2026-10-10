---
description: "Use when writing, modifying, or reviewing tests. Covers unit vs integration tests, mocking, naming and structure, test isolation, and the encryption test pattern."
paths:
  - "src/test/**"
---

# Test Conventions

The test-specific rules in `AGENTS.md` (`RateLimitServiceTest`,
`LoginAttemptServiceTest`, `ForeignKeyIndexIT`) and the AI test profile
(`ai-providers.md`) still apply.

## Unit and integration tests

- **`*Test`** classes run under `make test`. A test that needs CDI is a
  `@QuarkusTest`; pure logic (utilities, DTOs) is plain JUnit 5.
- **`*IT`** classes are `@QuarkusTest`s against the real PostgreSQL that Dev
  Services starts (Docker required). They run only with
  `./mvnw verify -DskipITs=false`, as CI does.
- One class or method: `./mvnw test -Dtest=AiTutorServiceTest[#method]`. A
  bare `./mvnw` needs the `.devkit` link, which any `make` target creates.

## Mocking

- Replace a CDI bean with Quarkus's `@InjectMock` (`quarkus-junit-mockito`),
  and stub with `Mockito.when(...)`.
- Mock AI providers at the JAX-RS client (`AbstractJaxRsAiServiceTest`
  mocks `Client`, `WebTarget` and `Invocation.Builder`), or run with
  `ai.tutor.provider=mock` / `ai.tutor.enabled=false`; no test calls a real
  AI API.

## Naming

Methods are `testMethodName` or `testMethodName_context`; most classes add a
`@DisplayName` sentence.

## Isolation

`@ApplicationScoped` services and the shared test database keep state across
tests, so test data uses unique identifiers (`UUID.randomUUID()`, or ULIDs
via `UlidUtil`), never fixed strings.

## Encryption tests

`EncryptionIT` asserts on what is stored, not on what the entity returns:
inject `DataSource`, read the raw column over JDBC, and check the versioned
envelope (`1|...`) and the blind index, rolling back with `@TestTransaction`
where the test writes. A test that deliberately passes `null` to a non-null
parameter carries `@SuppressWarnings("NullAway")` on that method.
