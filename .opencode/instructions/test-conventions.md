# Test Conventions — AIMathTutor

## Framework

- `@QuarkusTest` on all test classes
- Mockito via `quarkus-junit-mockito`
- Panache Mock via `quarkus-panache-mock` for entity mocking
- Docker required (Quarkus DevServices starts PostgreSQL automatically on port 55432)
- Test profile disables `@Retry` delays on Ollama calls and sets 1s timeouts

## Naming

- Test class: `<ClassName>Test` (e.g., `AiTutorServiceTest`, `LoginAttemptServiceTest`)
- Test method: `testMethodName` or `testMethodName_context` (e.g., `testAuthenticate_invalidPassword`)

## Running Tests

```shell
make test                                    # all tests (needs Docker)
./mvnw test                                  # equivalent
./mvnw test -Dtest=AiTutorServiceTest        # single class
./mvnw test -Dtest=AiTutorServiceTest#testGenerateHint  # single method
```

## Service Tests

```java
@QuarkusTest
class SomeServiceTest {
    @Inject
    SomeService someService;

    @Test
    void testSomeOperation() {
        // Arrange
        // Act
        // Assert
    }
}
```

- Use `@Inject` for the service under test
- Mock dependencies with Mockito `@Mock` + `@InjectMocks` where applicable
- Use `Mockito.when(...).thenReturn(...)` for stubbing
- Panache entities: use `PanacheMock.mock(Entity.class)` for static entity methods

## Important Test Constraints

- **RateLimitServiceTest**: Must use `UUID.randomUUID()` for user IDs. Hardcoded strings cause state leakage between tests since the service is `@ApplicationScoped`.
- **LoginAttemptServiceTest**: Must verify exact cap value of 3600 (not weak `<= 3600`).
- **Test data**: Use unique identifiers to avoid cross-test pollution. Prefer `UUID.randomUUID()` or ULIDs via `UlidUtil`.
- **Test profile overrides** (in `application.properties`):
  - `@Retry` delays disabled for `AiTutorService/callOllamaForQuestion` and `callOllamaForAnalysis`
  - Ollama client connect/read timeouts set to 1 second

## AI Provider Testing

- Mock provider: Set `ai.tutor.provider=mock` or `ai.tutor.enabled=false` for testing without external APIs
- Test profile: Ollama retry delays disabled, 1s timeouts — fail fast when Ollama unavailable
- When testing AI services: mock the underlying REST client responses

## Test Categories

| Category       | Approach                                                          |
| -------------- | ----------------------------------------------------------------- |
| Service tests  | `@QuarkusTest` + `@Inject` service + Mockito for dependencies     |
| Entity tests   | `PanacheMock` for static methods, `@TestTransaction` for DB tests |
| Security tests | Test password hashing via `PasswordHashingService`                |
| Utility tests  | Pure unit tests, no `@QuarkusTest` needed                         |
