---
description: "Use when writing, modifying, or reviewing tests. Covers unit vs integration tests, mocking, naming, test isolation, and the encryption test pattern."
paths:
  - "src/test/**"
---

# Test Conventions

The test-specific rules in `AGENTS.md` (`RateLimitServiceTest`,
`LoginAttemptServiceTest`, `ForeignKeyIndexIT`) and the AI test profile
(`ai-providers.md`) still apply.

## Unit and integration tests

- A test that needs CDI is a `@QuarkusTest`; pure logic (utilities, DTOs) is
  plain JUnit 5. Every `@QuarkusTest`, `*Test` or `*IT`, boots the app against
  the PostgreSQL that Dev Services starts, with Flyway migrating it, so those
  tests need Docker; plain JUnit tests do not.
- **`*Test`** classes run under `make test` (surefire). **`*IT`** classes run
  only under failsafe, with `./mvnw verify -DskipITs=false`, as CI does.
- One class or method: `./mvnw test -Dtest=AiTutorServiceTest[#method]`. A
  bare `./mvnw` needs the `.devkit` link, which any `make` target creates.

## Mocking

- Replace a CDI bean with Quarkus's `@InjectMock` (`quarkus-junit-mockito`),
  and stub with `Mockito.when(...)`.
- Mock AI providers at the JAX-RS client (`AbstractJaxRsAiServiceTest` mocks
  `Client`, `WebTarget` and `Invocation.Builder`), or run with
  `ai.tutor.provider=mock` / `ai.tutor.enabled=false`; no test calls a real AI
  API.

## Naming

Methods are mostly `testMethodName` or `testMethodName_context`; some larger
classes use behaviour names (`shouldLockOutAfterMaxFailedAttempts`). Most
classes add a `@DisplayName` sentence.

## Isolation

- A test that writes to the database rolls back with `@TestTransaction`.
- Where state outlives the test, use unique identifiers (`UUID.randomUUID()`,
  or ULIDs via `UlidUtil`), never fixed strings: in-memory state of
  `@ApplicationScoped` services (as in `RateLimitServiceTest`), and data a test
  commits.

## Encryption tests

`EncryptionIT` asserts on what is stored, not on what the entity returns:
inject `DataSource`, read the raw column over JDBC, and check the versioned
envelope (`1|...`) and the blind index, rolling back with `@TestTransaction`
where the test writes. Its one test that deliberately passes `null` to a
non-null parameter carries `@SuppressWarnings("NullAway")` on that method;
many other test classes carry the suppression at class level.
