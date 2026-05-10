# Test Conventions — AIMathTutor

## Framework

- **Integration tests** (`*IT` classes): `@QuarkusTest` + `@Inject` for CDI beans; full Quarkus container with real DB via DevServices
- **Service / entity tests** (`*Test` classes): `@QuarkusTest` + Mockito / PanacheMock; container still required for CDI
- **Utility tests** (pure logic, no container needed): plain JUnit 5 — `@QuarkusTest` not required
- Mockito via `quarkus-junit-mockito`
- Panache Mock via `quarkus-panache-mock` for entity mocking
- Docker required (Quarkus DevServices starts PostgreSQL automatically on port 55432)
- Test profile disables `@Retry` delays on Ollama calls and sets 1s timeouts

## Naming

- Integration test class: `<ClassName>IT` (e.g., `EncryptionIT`, `UserRepositoryIT`) — `@QuarkusTest` with real DB, JDBC assertions, or CDI injection
- Service/entity test class: `<ClassName>Test` (e.g., `AiTutorServiceTest`, `LoginAttemptServiceTest`)
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

| Category       | Approach                                                                                                      |
| -------------- | ------------------------------------------------------------------------------------------------------------- |
| Service tests  | `@QuarkusTest` + `@Inject` service + Mockito for dependencies                                                 |
| Entity tests   | `PanacheMock` for static methods, `@TestTransaction` for DB tests                                             |
| Security tests | Test password hashing via `PasswordHashingService`                                                            |
| Utility tests  | Plain JUnit 5 — no `@QuarkusTest` needed                                                                      |
| Encryption ITs | `@QuarkusTest` + `@Inject DataSource` for raw JDBC; verify ciphertext envelope format and blind-index storage |

## Encryption Integration Tests

`EncryptionIT` pattern: inject `DataSource` and read raw column values via JDBC to assert that plaintext is never stored. Use `@TestTransaction` to roll back after each test. Pass `@Nullable String email` to helper methods — use `@SuppressWarnings("NullAway")` **on the specific test method** (not the whole class) that deliberately passes `null` to a `@NonNull` parameter (same pattern as `UserRepositoryIT`).

```java
@Test
@TestTransaction
void testEmailStoredAsCiphertext() throws SQLException {
    // persist user via repository, then read raw value via DataSource
    try (Connection c = dataSource.getConnection();
         PreparedStatement ps = c.prepareStatement("SELECT email FROM users WHERE username = ?")) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
            String raw = rs.next() ? rs.getString(1) : null;
            assertNotNull(raw);
            assertTrue(raw.startsWith("1|")); // versioned envelope
        }
    }
}
```
